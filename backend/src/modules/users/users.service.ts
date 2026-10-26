import crypto from 'crypto';
import argon2 from 'argon2';
import ExcelJS from 'exceljs';
import { Prisma, UserRole } from '@prisma/client';
import { prisma } from '../../lib/prisma';
import { AppError } from '../../errors/appError';
import { logAudit } from '../../lib/audit';
import { CreateUserInput, UpdateUserInput, UserQueryInput } from './users.schema';

export class UsersService {
  async getMe(userId: string) {
    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        nama: true,
        nomor_induk: true,
        email: true,
        role: true,
        is_active: true,
        created_at: true,
      },
    });

    if (!user) {
      throw new AppError('USER_NOT_FOUND', 'Pengguna tidak ditemukan', 404);
    }

    return user;
  }

  async getUsers(query: UserQueryInput) {
    const skip = (query.page - 1) * query.pageSize;
    const take = query.pageSize;

    const where: Prisma.UserWhereInput = {};

    if (query.role) {
      where.role = query.role as UserRole;
    }

    if (query.is_active !== undefined) {
      where.is_active = query.is_active;
    }

    if (query.q) {
      const search = query.q.trim();
      where.OR = [
        { nama: { contains: search, mode: 'insensitive' } },
        { nomor_induk: { contains: search, mode: 'insensitive' } },
        { email: { contains: search, mode: 'insensitive' } },
      ];
    }

    const [total, users] = await Promise.all([
      prisma.user.count({ where }),
      prisma.user.findMany({
        where,
        skip,
        take,
        select: {
          id: true,
          nama: true,
          nomor_induk: true,
          email: true,
          role: true,
          is_active: true,
          created_at: true,
        },
        orderBy: { created_at: 'desc' },
      }),
    ]);

    const pagination = {
      page: query.page,
      pageSize: query.pageSize,
      total,
      totalPages: Math.ceil(total / query.pageSize) || 1,
    };

    return {
      data: users,
      pagination,
      meta: pagination,
    };
  }

  async createUser(input: CreateUserInput, operatorId: string) {
    const existing = await prisma.user.findFirst({
      where: {
        OR: [
          { nomor_induk: input.nomor_induk },
          ...(input.email ? [{ email: input.email }] : []),
        ],
      },
    });

    if (existing) {
      if (existing.nomor_induk === input.nomor_induk) {
        throw new AppError('CONFLICT', 'Nomor induk sudah terdaftar', 409);
      }
      throw new AppError('CONFLICT', 'Email sudah terdaftar', 409);
    }

    const rawPassword = input.password || crypto.randomBytes(6).toString('hex');
    const passwordHash = await argon2.hash(rawPassword, {
      type: argon2.argon2id,
    });

    const user = await prisma.user.create({
      data: {
        nama: input.nama,
        nomor_induk: input.nomor_induk,
        email: input.email,
        role: input.role as UserRole,
        password_hash: passwordHash,
      },
      select: {
        id: true,
        nama: true,
        nomor_induk: true,
        email: true,
        role: true,
        is_active: true,
        created_at: true,
      },
    });

    await logAudit({
      actor_id: operatorId,
      action: 'USER_CREATED',
      entity: 'User',
      entity_id: user.id,
      new_value: { nama: user.nama, nomor_induk: user.nomor_induk, role: user.role },
    });

    return {
      user,
      initialPassword: rawPassword,
    };
  }

  async updateUser(userId: string, input: UpdateUserInput, operatorId: string) {
    const user = await prisma.user.findUnique({
      where: { id: userId },
    });

    if (!user) {
      throw new AppError('USER_NOT_FOUND', 'Pengguna tidak ditemukan', 404);
    }

    if (input.email && input.email !== user.email) {
      const emailExists = await prisma.user.findUnique({
        where: { email: input.email },
      });
      if (emailExists) {
        throw new AppError('CONFLICT', 'Email sudah terdaftar pada akun lain', 409);
      }
    }

    const updated = await prisma.user.update({
      where: { id: userId },
      data: {
        nama: input.nama,
        email: input.email,
        role: input.role as UserRole | undefined,
        is_active: input.is_active,
      },
      select: {
        id: true,
        nama: true,
        nomor_induk: true,
        email: true,
        role: true,
        is_active: true,
      },
    });

    await logAudit({
      actor_id: operatorId,
      action: 'USER_UPDATED',
      entity: 'User',
      entity_id: userId,
      old_value: { nama: user.nama, role: user.role, is_active: user.is_active },
      new_value: input,
    });

    return updated;
  }

  async resetPassword(userId: string, operatorId: string) {
    const user = await prisma.user.findUnique({
      where: { id: userId },
    });

    if (!user) {
      throw new AppError('USER_NOT_FOUND', 'Pengguna tidak ditemukan', 404);
    }

    const temporaryPassword = crypto.randomBytes(6).toString('hex');
    const passwordHash = await argon2.hash(temporaryPassword, {
      type: argon2.argon2id,
    });

    await prisma.$transaction([
      prisma.user.update({
        where: { id: userId },
        data: { password_hash: passwordHash },
      }),
      prisma.refreshToken.updateMany({
        where: { user_id: userId, revoked_at: null },
        data: { revoked_at: new Date() },
      }),
    ]);

    await logAudit({
      actor_id: operatorId,
      action: 'RESET_PASSWORD',
      entity: 'User',
      entity_id: userId,
    });

    return {
      temporaryPassword,
      message: 'Password berhasil direset',
    };
  }

  async generateImportTemplate(): Promise<Buffer> {
    const workbook = new ExcelJS.Workbook();
    workbook.creator = 'Sistem Absensi';
    const sheet = workbook.addWorksheet('Template Pengguna');

    sheet.columns = [
      { header: 'role', key: 'role', width: 15 },
      { header: 'nama', key: 'nama', width: 30 },
      { header: 'nomor_induk', key: 'nomor_induk', width: 20 },
      { header: 'email', key: 'email', width: 30 },
    ];

    sheet.addRow({
      role: 'guru',
      nama: 'Budi Santoso, M.Pd',
      nomor_induk: '198501012010011001',
      email: 'budi@sekolah.sch.id',
    });
    sheet.addRow({
      role: 'siswa',
      nama: 'Ahmad Dahlan',
      nomor_induk: '202401001',
      email: 'ahmad@sekolah.sch.id',
    });

    const buffer = await workbook.xlsx.writeBuffer();
    return Buffer.from(buffer);
  }

  async importUsers(fileBuffer: Buffer, operatorId: string) {
    const workbook = new ExcelJS.Workbook();
    await (workbook.xlsx.load as unknown as (buffer: Buffer) => Promise<ExcelJS.Workbook>)(fileBuffer);
    const sheet = workbook.worksheets[0];
    if (!sheet) {
      throw new AppError('INVALID_FILE', 'File Excel tidak memiliki worksheet yang valid', 400);
    }

    interface RowReport {
      row: number;
      role?: string;
      nama?: string;
      nomor_induk?: string;
      email?: string;
      status: 'berhasil' | 'gagal';
      reason?: string;
      initial_password?: string;
    }

    const rowReports: RowReport[] = [];
    const validUsersToCreate: {
      row: number;
      role: UserRole;
      nama: string;
      nomor_induk: string;
      email: string | null;
      initialPassword: string;
      passwordHash: string;
    }[] = [];

    const seenNomorInduk = new Set<string>();
    const seenEmail = new Set<string>();

    const rowsCount = sheet.rowCount;
    if (rowsCount < 2) {
      throw new AppError('EMPTY_FILE', 'File Excel tidak memiliki data pengguna', 400);
    }

    for (let r = 2; r <= rowsCount; r++) {
      const row = sheet.getRow(r);
      const roleRaw = row.getCell(1).text?.trim().toLowerCase();
      const nama = row.getCell(2).text?.trim();
      const nomorInduk = row.getCell(3).text?.trim();
      const emailRaw = row.getCell(4).text?.trim();
      const email = emailRaw ? emailRaw.toLowerCase() : null;

      if (!roleRaw && !nama && !nomorInduk && !emailRaw) {
        continue;
      }

      if (!nama || !nomorInduk || !roleRaw) {
        rowReports.push({
          row: r,
          role: roleRaw,
          nama,
          nomor_induk: nomorInduk,
          email: email ?? undefined,
          status: 'gagal',
          reason: 'Kolom role, nama, dan nomor_induk wajib diisi',
        });
        continue;
      }

      if (!['operator', 'guru', 'siswa'].includes(roleRaw)) {
        rowReports.push({
          row: r,
          role: roleRaw,
          nama,
          nomor_induk: nomorInduk,
          email: email ?? undefined,
          status: 'gagal',
          reason: `Role '${roleRaw}' tidak valid (pilihan: operator, guru, siswa)`,
        });
        continue;
      }

      if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        rowReports.push({
          row: r,
          role: roleRaw,
          nama,
          nomor_induk: nomorInduk,
          email,
          status: 'gagal',
          reason: 'Format email tidak valid',
        });
        continue;
      }

      if (seenNomorInduk.has(nomorInduk)) {
        rowReports.push({
          row: r,
          role: roleRaw,
          nama,
          nomor_induk: nomorInduk,
          email: email ?? undefined,
          status: 'gagal',
          reason: 'Nomor induk duplikat di dalam file impor',
        });
        continue;
      }
      seenNomorInduk.add(nomorInduk);

      if (email) {
        if (seenEmail.has(email)) {
          rowReports.push({
            row: r,
            role: roleRaw,
            nama,
            nomor_induk: nomorInduk,
            email,
            status: 'gagal',
            reason: 'Email duplikat di dalam file impor',
          });
          continue;
        }
        seenEmail.add(email);
      }

      // Periksa keunikan di database
      const existingUser = await prisma.user.findFirst({
        where: {
          OR: [{ nomor_induk: nomorInduk }, ...(email ? [{ email }] : [])],
        },
      });

      if (existingUser) {
        const conflictField = existingUser.nomor_induk === nomorInduk ? 'Nomor induk' : 'Email';
        rowReports.push({
          row: r,
          role: roleRaw,
          nama,
          nomor_induk: nomorInduk,
          email: email ?? undefined,
          status: 'gagal',
          reason: `${conflictField} sudah terdaftar di sistem`,
        });
        continue;
      }

      // Buat password awal acak
      const initialPassword = crypto.randomBytes(6).toString('hex');
      const passwordHash = await argon2.hash(initialPassword, {
        type: argon2.argon2id,
      });

      validUsersToCreate.push({
        row: r,
        role: roleRaw as UserRole,
        nama,
        nomor_induk: nomorInduk,
        email,
        initialPassword,
        passwordHash,
      });
    }

    // Proses dalam transaksi per batch
    const BATCH_SIZE = 50;
    for (let i = 0; i < validUsersToCreate.length; i += BATCH_SIZE) {
      const batch = validUsersToCreate.slice(i, i + BATCH_SIZE);
      await prisma.$transaction(
        batch.map((u) =>
          prisma.user.create({
            data: {
              role: u.role,
              nama: u.nama,
              nomor_induk: u.nomor_induk,
              email: u.email,
              password_hash: u.passwordHash,
              is_active: true,
            },
          })
        )
      );
    }

    for (const u of validUsersToCreate) {
      rowReports.push({
        row: u.row,
        role: u.role,
        nama: u.nama,
        nomor_induk: u.nomor_induk,
        email: u.email ?? undefined,
        status: 'berhasil',
        initial_password: u.initialPassword,
      });
    }

    rowReports.sort((a, b) => a.row - b.row);

    // Buat file Excel hasil impor dengan kredensial awal
    const resultWorkbook = new ExcelJS.Workbook();
    const resultSheet = resultWorkbook.addWorksheet('Hasil Impor');
    resultSheet.columns = [
      { header: 'Baris', key: 'row', width: 10 },
      { header: 'Role', key: 'role', width: 15 },
      { header: 'Nama', key: 'nama', width: 30 },
      { header: 'Nomor Induk', key: 'nomor_induk', width: 20 },
      { header: 'Email', key: 'email', width: 30 },
      { header: 'Status', key: 'status', width: 15 },
      { header: 'Alasan', key: 'reason', width: 35 },
      { header: 'Password Awal', key: 'initial_password', width: 20 },
    ];

    for (const rep of rowReports) {
      resultSheet.addRow(rep);
    }

    const resultBufferRaw = await resultWorkbook.xlsx.writeBuffer();
    const resultBuffer = Buffer.from(resultBufferRaw);

    await logAudit({
      actor_id: operatorId,
      action: 'IMPORT_USERS',
      entity: 'User',
      new_value: {
        total_rows: rowReports.length,
        success: validUsersToCreate.length,
        failed: rowReports.length - validUsersToCreate.length,
      },
    });

    return {
      summary: {
        total: rowReports.length,
        success: validUsersToCreate.length,
        failed: rowReports.length - validUsersToCreate.length,
      },
      rows: rowReports,
      excelBuffer: resultBuffer,
    };
  }
}

export const usersService = new UsersService();
