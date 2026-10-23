import { Prisma } from '@prisma/client';
import { prisma } from '../../lib/prisma';
import { AppError } from '../../errors/appError';
import { CreateScheduleInput, UpdateScheduleInput, ListScheduleQuery } from './schedules.schema';

export function timeStringToMinutes(timeStr: string): number {
  const parts = timeStr.split(':');
  const h = parseInt(parts[0], 10);
  const m = parseInt(parts[1], 10);
  return h * 60 + m;
}

export function dateToTimeString(date: Date): string {
  const h = String(date.getUTCHours()).padStart(2, '0');
  const m = String(date.getUTCMinutes()).padStart(2, '0');
  return `${h}:${m}`;
}

export function timeStringToDate(timeStr: string): Date {
  const parts = timeStr.split(':');
  const h = parts[0].padStart(2, '0');
  const m = parts[1].padStart(2, '0');
  const s = parts[2] ? parts[2].padStart(2, '0') : '00';
  return new Date(`1970-01-01T${h}:${m}:${s}.000Z`);
}

function extractMinutes(val: Date | string): number {
  if (val instanceof Date) {
    return val.getUTCHours() * 60 + val.getUTCMinutes();
  }
  return timeStringToMinutes(String(val));
}

export class SchedulesService {
  static async listSchedules(query: ListScheduleQuery) {
    const { page, pageSize, class_id, teacher_id, day_of_week, q } = query;
    const skip = (page - 1) * pageSize;

    const where: Prisma.ScheduleWhereInput = {};
    if (class_id) where.class_id = class_id;
    if (teacher_id) where.teacher_id = teacher_id;
    if (day_of_week) where.day_of_week = day_of_week;
    if (q) {
      where.OR = [
        { class: { nama: { contains: q, mode: 'insensitive' } } },
        { subject: { nama: { contains: q, mode: 'insensitive' } } },
        { teacher: { nama: { contains: q, mode: 'insensitive' } } },
        { location: { nama: { contains: q, mode: 'insensitive' } } },
      ];
    }

    const [total, items] = await Promise.all([
      prisma.schedule.count({ where }),
      prisma.schedule.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: [{ day_of_week: 'asc' }, { start_time: 'asc' }],
        include: {
          class: { select: { id: true, nama: true, tahun_ajaran: true } },
          subject: { select: { id: true, nama: true } },
          teacher: { select: { id: true, nama: true, nomor_induk: true, email: true } },
          location: { select: { id: true, nama: true, latitude: true, longitude: true, radius_m: true, is_active: true } },
        },
      }),
    ]);

    const formattedItems = items.map((s) => ({
      ...s,
      start_time: dateToTimeString(s.start_time),
      end_time: dateToTimeString(s.end_time),
    }));

    return {
      items: formattedItems,
      pagination: {
        page,
        pageSize,
        total,
        totalPages: Math.ceil(total / pageSize) || 1,
      },
    };
  }

  static async getScheduleById(id: string) {
    const schedule = await prisma.schedule.findUnique({
      where: { id },
      include: {
        class: { select: { id: true, nama: true, tahun_ajaran: true } },
        subject: { select: { id: true, nama: true } },
        teacher: { select: { id: true, nama: true, nomor_induk: true, email: true } },
        location: { select: { id: true, nama: true, latitude: true, longitude: true, radius_m: true, is_active: true } },
      },
    });

    if (!schedule) {
      throw new AppError('NOT_FOUND', 'Jadwal tidak ditemukan', 404);
    }

    return {
      ...schedule,
      start_time: dateToTimeString(schedule.start_time),
      end_time: dateToTimeString(schedule.end_time),
    };
  }

  static async createSchedule(input: CreateScheduleInput) {
    // 1. Validasi guru
    const teacher = await prisma.user.findUnique({
      where: { id: input.teacher_id },
    });
    if (!teacher) {
      throw new AppError('NOT_FOUND', 'Guru tidak ditemukan', 404);
    }
    if (teacher.role !== 'guru') {
      throw new AppError('INVALID_TEACHER_ROLE', 'Pengguna yang dipilih bukan seorang guru', 400);
    }
    if (!teacher.is_active) {
      throw new AppError('INACTIVE_TEACHER', 'Guru yang dipilih berstatus tidak aktif', 400);
    }

    // 2. Validasi kelas
    const classItem = await prisma.class.findUnique({
      where: { id: input.class_id },
    });
    if (!classItem) {
      throw new AppError('NOT_FOUND', 'Kelas tidak ditemukan', 404);
    }

    // 3. Validasi mapel
    const subject = await prisma.subject.findUnique({
      where: { id: input.subject_id },
    });
    if (!subject) {
      throw new AppError('NOT_FOUND', 'Mata pelajaran tidak ditemukan', 404);
    }

    // 4. Validasi lokasi
    const location = await prisma.location.findUnique({
      where: { id: input.location_id },
    });
    if (!location) {
      throw new AppError('NOT_FOUND', 'Lokasi tidak ditemukan', 404);
    }
    if (!location.is_active) {
      throw new AppError('INACTIVE_LOCATION', 'Lokasi yang dipilih berstatus tidak aktif', 400);
    }

    // 5. Cek bentrok jadwal guru (SCHEDULE_CONFLICT)
    const newStartM = timeStringToMinutes(input.start_time);
    const newEndM = timeStringToMinutes(input.end_time);

    const existingSchedules = await prisma.schedule.findMany({
      where: {
        teacher_id: input.teacher_id,
        day_of_week: input.day_of_week,
      },
    });

    for (const existing of existingSchedules) {
      const existingStartM = extractMinutes(existing.start_time);
      const existingEndM = extractMinutes(existing.end_time);

      if (newStartM < existingEndM && existingStartM < newEndM) {
        throw new AppError(
          'SCHEDULE_CONFLICT',
          'Terdapat bentrok jadwal guru yang sama pada hari dan jam yang beririsan',
          409
        );
      }
    }

    const created = await prisma.schedule.create({
      data: {
        class_id: input.class_id,
        subject_id: input.subject_id,
        teacher_id: input.teacher_id,
        location_id: input.location_id,
        day_of_week: input.day_of_week,
        start_time: timeStringToDate(input.start_time),
        end_time: timeStringToDate(input.end_time),
      },
    });

    return this.getScheduleById(created.id);
  }

  static async updateSchedule(id: string, input: UpdateScheduleInput) {
    const existingSchedule = await prisma.schedule.findUnique({
      where: { id },
    });
    if (!existingSchedule) {
      throw new AppError('NOT_FOUND', 'Jadwal tidak ditemukan', 404);
    }

    const teacherId = input.teacher_id ?? existingSchedule.teacher_id;
    const dayOfWeek = input.day_of_week ?? existingSchedule.day_of_week;
    const startTimeStr = input.start_time ?? dateToTimeString(existingSchedule.start_time);
    const endTimeStr = input.end_time ?? dateToTimeString(existingSchedule.end_time);

    if (input.teacher_id) {
      const teacher = await prisma.user.findUnique({
        where: { id: input.teacher_id },
      });
      if (!teacher) {
        throw new AppError('NOT_FOUND', 'Guru tidak ditemukan', 404);
      }
      if (teacher.role !== 'guru') {
        throw new AppError('INVALID_TEACHER_ROLE', 'Pengguna yang dipilih bukan seorang guru', 400);
      }
      if (!teacher.is_active) {
        throw new AppError('INACTIVE_TEACHER', 'Guru yang dipilih berstatus tidak aktif', 400);
      }
    }

    if (input.location_id) {
      const location = await prisma.location.findUnique({
        where: { id: input.location_id },
      });
      if (!location) {
        throw new AppError('NOT_FOUND', 'Lokasi tidak ditemukan', 404);
      }
      if (!location.is_active) {
        throw new AppError('INACTIVE_LOCATION', 'Lokasi yang dipilih berstatus tidak aktif', 400);
      }
    }

    // Cek bentrok jadwal
    const newStartM = timeStringToMinutes(startTimeStr);
    const newEndM = timeStringToMinutes(endTimeStr);

    const otherSchedules = await prisma.schedule.findMany({
      where: {
        teacher_id: teacherId,
        day_of_week: dayOfWeek,
        id: { not: id },
      },
    });

    for (const other of otherSchedules) {
      const otherStartM = extractMinutes(other.start_time);
      const otherEndM = extractMinutes(other.end_time);

      if (newStartM < otherEndM && otherStartM < newEndM) {
        throw new AppError(
          'SCHEDULE_CONFLICT',
          'Terdapat bentrok jadwal guru yang sama pada hari dan jam yang beririsan',
          409
        );
      }
    }

    const dataToUpdate: Prisma.ScheduleUpdateInput = {};
    if (input.class_id) dataToUpdate.class = { connect: { id: input.class_id } };
    if (input.subject_id) dataToUpdate.subject = { connect: { id: input.subject_id } };
    if (input.teacher_id) dataToUpdate.teacher = { connect: { id: input.teacher_id } };
    if (input.location_id) dataToUpdate.location = { connect: { id: input.location_id } };
    if (input.day_of_week) dataToUpdate.day_of_week = input.day_of_week;
    if (input.start_time) dataToUpdate.start_time = timeStringToDate(input.start_time);
    if (input.end_time) dataToUpdate.end_time = timeStringToDate(input.end_time);

    await prisma.schedule.update({
      where: { id },
      data: dataToUpdate,
    });

    return this.getScheduleById(id);
  }

  static async deleteSchedule(id: string) {
    const schedule = await prisma.schedule.findUnique({
      where: { id },
    });
    if (!schedule) {
      throw new AppError('NOT_FOUND', 'Jadwal tidak ditemukan', 404);
    }

    const sessionsCount = await prisma.attendanceSession.count({
      where: { schedule_id: id },
    });
    if (sessionsCount > 0) {
      throw new AppError('IN_USE', 'Jadwal masih digunakan dalam sesi absensi dan tidak dapat dihapus', 409);
    }

    return prisma.schedule.delete({
      where: { id },
    });
  }

  static async getMeSchedules(userId: string, role: string) {
    let where: Prisma.ScheduleWhereInput = {};

    if (role === 'guru') {
      where = { teacher_id: userId };
    } else if (role === 'siswa') {
      const memberships = await prisma.classMember.findMany({
        where: { user_id: userId },
        select: { class_id: true },
      });
      const classIds = memberships.map((m) => m.class_id);
      where = { class_id: { in: classIds } };
    } else {
      where = {};
    }

    const schedules = await prisma.schedule.findMany({
      where,
      orderBy: [{ day_of_week: 'asc' }, { start_time: 'asc' }],
      include: {
        class: { select: { id: true, nama: true, tahun_ajaran: true } },
        subject: { select: { id: true, nama: true } },
        teacher: { select: { id: true, nama: true, nomor_induk: true } },
        location: { select: { id: true, nama: true, latitude: true, longitude: true, radius_m: true } },
      },
    });

    return schedules.map((s) => ({
      ...s,
      start_time: dateToTimeString(s.start_time),
      end_time: dateToTimeString(s.end_time),
    }));
  }
}
