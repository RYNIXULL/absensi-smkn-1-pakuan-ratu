import { Prisma } from '@prisma/client';
import { prisma } from '../../lib/prisma';
import { AppError } from '../../errors/appError';
import { CreateSubjectInput, UpdateSubjectInput, ListSubjectQuery } from './subjects.schema';

export class SubjectsService {
  static async listSubjects(query: ListSubjectQuery) {
    const { page, pageSize, q } = query;
    const skip = (page - 1) * pageSize;

    const where: Prisma.SubjectWhereInput = {};
    if (q) {
      where.nama = { contains: q, mode: 'insensitive' };
    }

    const [total, items] = await Promise.all([
      prisma.subject.count({ where }),
      prisma.subject.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: { nama: 'asc' },
        include: {
          _count: {
            select: { schedules: true },
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

  static async getSubjectById(id: string) {
    const subject = await prisma.subject.findUnique({
      where: { id },
      include: {
        _count: {
          select: { schedules: true },
        },
      },
    });

    if (!subject) {
      throw new AppError('NOT_FOUND', 'Mata pelajaran tidak ditemukan', 404);
    }

    return subject;
  }

  static async createSubject(input: CreateSubjectInput) {
    return prisma.subject.create({
      data: {
        nama: input.nama,
      },
    });
  }

  static async updateSubject(id: string, input: UpdateSubjectInput) {
    await this.getSubjectById(id);

    return prisma.subject.update({
      where: { id },
      data: input,
    });
  }

  static async deleteSubject(id: string) {
    await this.getSubjectById(id);

    const schedulesCount = await prisma.schedule.count({
      where: { subject_id: id },
    });
    if (schedulesCount > 0) {
      throw new AppError('IN_USE', 'Mata pelajaran masih digunakan dalam jadwal dan tidak dapat dihapus', 409);
    }

    return prisma.subject.delete({
      where: { id },
    });
  }
}
