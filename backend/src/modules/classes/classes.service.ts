import { Prisma } from '@prisma/client';
import { prisma } from '../../lib/prisma';
import { AppError } from '../../errors/appError';
import { CreateClassInput, UpdateClassInput, ListClassQuery } from './classes.schema';

export class ClassesService {
  static async listClasses(query: ListClassQuery) {
    const { page, pageSize, q, tahun_ajaran } = query;
    const skip = (page - 1) * pageSize;

    const where: Prisma.ClassWhereInput = {};
    if (q) {
      where.OR = [
        { nama: { contains: q, mode: 'insensitive' } },
        { tahun_ajaran: { contains: q, mode: 'insensitive' } },
      ];
    }
    if (tahun_ajaran) {
      where.tahun_ajaran = tahun_ajaran;
    }

    const [total, items] = await Promise.all([
      prisma.class.count({ where }),
      prisma.class.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: [{ tahun_ajaran: 'desc' }, { nama: 'asc' }],
        include: {
          _count: {
            select: { members: true, schedules: true },
          },
        },
      }),
    ]);

    return {
      items,
      pagination: {
        page,
        pageSize,
        total,
        totalPages: Math.ceil(total / pageSize) || 1,
      },
    };
  }

  static async getClassById(id: string) {
    const classItem = await prisma.class.findUnique({
      where: { id },
      include: {
        _count: {
          select: { members: true, schedules: true },
        },
      },
    });

    if (!classItem) {
      throw new AppError('NOT_FOUND', 'Kelas tidak ditemukan', 404);
    }

    return classItem;
  }

  static async createClass(input: CreateClassInput) {
    return prisma.class.create({
      data: {
        nama: input.nama,
        tahun_ajaran: input.tahun_ajaran,
      },
    });
  }

  static async updateClass(id: string, input: UpdateClassInput) {
    await this.getClassById(id);

    return prisma.class.update({
      where: { id },
      data: input,
    });
  }

  static async deleteClass(id: string) {
    await this.getClassById(id);

    // Cek referensi jadwal
    const schedulesCount = await prisma.schedule.count({
      where: { class_id: id },
    });
    if (schedulesCount > 0) {
      throw new AppError('IN_USE', 'Kelas masih digunakan dalam jadwal dan tidak dapat dihapus', 409);
    }

    // Cek referensi anggota
    const membersCount = await prisma.classMember.count({
      where: { class_id: id },
    });
    if (membersCount > 0) {
      throw new AppError('IN_USE', 'Kelas masih memiliki anggota terdaftar dan tidak dapat dihapus', 409);
    }

    return prisma.class.delete({
      where: { id },
    });
  }

  static async listMembers(classId: string) {
    await this.getClassById(classId);

    const members = await prisma.classMember.findMany({
      where: { class_id: classId },
      include: {
        user: {
          select: {
            id: true,
            nama: true,
            nomor_induk: true,
            role: true,
            email: true,
            is_active: true,
          },
        },
      },
      orderBy: {
        user: {
          nama: 'asc',
        },
      },
    });

    return members.map((m) => ({
      id: m.id,
      user_id: m.user_id,
      class_id: m.class_id,
      user: m.user,
    }));
  }

  static async addMembers(classId: string, userIds: string[]) {
    await this.getClassById(classId);

    const uniqueUserIds = Array.from(new Set(userIds));

    const users = await prisma.user.findMany({
      where: {
        id: { in: uniqueUserIds },
      },
    });

    if (users.length !== uniqueUserIds.length) {
      throw new AppError('USER_NOT_FOUND', 'Satu atau lebih pengguna tidak ditemukan', 400);
    }

    for (const u of users) {
      if (u.role !== 'siswa' && u.role !== 'guru') {
        throw new AppError('INVALID_MEMBER_ROLE', `Pengguna ${u.nama} bukan siswa atau guru`, 400);
      }
    }

    const existingMembers = await prisma.classMember.findMany({
      where: {
        class_id: classId,
        user_id: { in: uniqueUserIds },
      },
    });

    if (existingMembers.length > 0) {
      throw new AppError('DUPLICATE_MEMBER', 'Satu atau lebih pengguna sudah terdaftar sebagai anggota kelas', 409);
    }

    const created = await prisma.$transaction(
      uniqueUserIds.map((userId) =>
        prisma.classMember.create({
          data: {
            class_id: classId,
            user_id: userId,
          },
        })
      )
    );

    return { count: created.length, members: created };
  }

  static async removeMember(classId: string, userId: string) {
    await this.getClassById(classId);

    const member = await prisma.classMember.findUnique({
      where: {
        class_id_user_id: {
          class_id: classId,
          user_id: userId,
        },
      },
    });

    if (!member) {
      throw new AppError('MEMBER_NOT_FOUND', 'Pengguna bukan merupakan anggota kelas ini', 404);
    }

    return prisma.classMember.delete({
      where: {
        id: member.id,
      },
    });
  }
}
