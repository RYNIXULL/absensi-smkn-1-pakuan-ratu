import crypto from 'crypto';
import argon2 from 'argon2';
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

    return {
      data: users,
      meta: {
        page: query.page,
        pageSize: query.pageSize,
        total,
        totalPages: Math.ceil(total / query.pageSize),
      },
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
}

export const usersService = new UsersService();
