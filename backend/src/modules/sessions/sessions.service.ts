import crypto from 'crypto';
import { prisma } from '../../lib/prisma';
import { AppError } from '../../errors/appError';
import { SessionStatus, UserRole, Prisma } from '@prisma/client';
import {
  getJakartaDateInfo,
  parseTimeToMinutes,
  createUtcFromJakartaTime,
} from '../../lib/dateUtils';
import { DEFAULT_LATE_AFTER_MIN, DEFAULT_OPEN_BEFORE_MIN } from '../../jobs/sessionManager';

export class SessionsService {
  /**
   * Buka sesi manual oleh operator atau guru pengampu jadwalnya.
   */
  async openManualSession(
    userId: string,
    userRole: string,
    data: {
      schedule_id: string;
      tanggal?: string;
      open_at?: string;
      close_at?: string;
      late_after?: string;
    }
  ) {
    const schedule = await prisma.schedule.findUnique({
      where: { id: data.schedule_id },
      include: { class: true },
    });

    if (!schedule) {
      throw new AppError('NOT_FOUND', 'Jadwal pelajaran tidak ditemukan', 404);
    }

    if (userRole === UserRole.guru && schedule.teacher_id !== userId) {
      throw new AppError(
        'FORBIDDEN',
        'Guru hanya dapat membuka sesi untuk jadwal kelas yang diampunya',
        403
      );
    }

    const now = new Date();
    const jakartaToday = getJakartaDateInfo(now);
    const dateStr = data.tanggal || jakartaToday.dateString;
    const dateOnly = new Date(`${dateStr}T00:00:00.000Z`);

    const startMin = parseTimeToMinutes(schedule.start_time);
    const endMin = parseTimeToMinutes(schedule.end_time);

    const openAt = data.open_at
      ? new Date(data.open_at)
      : createUtcFromJakartaTime(dateStr, startMin - DEFAULT_OPEN_BEFORE_MIN);
    const lateAfter = data.late_after
      ? new Date(data.late_after)
      : createUtcFromJakartaTime(dateStr, startMin + DEFAULT_LATE_AFTER_MIN);
    const closeAt = data.close_at
      ? new Date(data.close_at)
      : createUtcFromJakartaTime(dateStr, endMin);

    // Cek apakah sesi sudah ada untuk (schedule_id, tanggal)
    const existing = await prisma.attendanceSession.findUnique({
      where: {
        schedule_id_tanggal: {
          schedule_id: data.schedule_id,
          tanggal: dateOnly,
        },
      },
    });

    if (existing) {
      // Buka sesi yang sudah ada
      const updated = await prisma.attendanceSession.update({
        where: { id: existing.id },
        data: {
          status: SessionStatus.terbuka,
          opened_by: userId,
          open_at: openAt,
          late_after: lateAfter,
          close_at: closeAt,
        },
        include: {
          schedule: {
            include: {
              class: true,
              subject: true,
              teacher: { select: { id: true, nama: true } },
              location: true,
            },
          },
        },
      });

      await prisma.auditLog.create({
        data: {
          actor_id: userId,
          action: 'SESSION_OPENED_MANUAL',
          entity: 'attendance_sessions',
          entity_id: updated.id,
          old_value: { status: existing.status },
          new_value: { status: updated.status, opened_by: userId },
        },
      });

      return updated;
    }

    // Buat sesi baru langsung status 'terbuka'
    const newSession = await prisma.attendanceSession.create({
      data: {
        schedule_id: data.schedule_id,
        tanggal: dateOnly,
        open_at: openAt,
        late_after: lateAfter,
        close_at: closeAt,
        status: SessionStatus.terbuka,
        opened_by: userId,
      },
      include: {
        schedule: {
          include: {
            class: true,
            subject: true,
            teacher: { select: { id: true, nama: true } },
            location: true,
          },
        },
      },
    });

    await prisma.auditLog.create({
      data: {
        actor_id: userId,
        action: 'SESSION_OPENED_MANUAL',
        entity: 'attendance_sessions',
        entity_id: newSession.id,
        new_value: { status: newSession.status, opened_by: userId },
      },
    });

    return newSession;
  }

  /**
   * Tutup sesi oleh operator atau guru kelasnya
   */
  async closeSession(sessionId: string, userId: string, userRole: string) {
    const session = await prisma.attendanceSession.findUnique({
      where: { id: sessionId },
      include: { schedule: true },
    });

    if (!session) {
      throw new AppError('NOT_FOUND', 'Sesi absensi tidak ditemukan', 404);
    }

    if (userRole === UserRole.guru && session.schedule.teacher_id !== userId) {
      throw new AppError(
        'FORBIDDEN',
        'Guru hanya dapat menutup sesi untuk jadwal kelas yang diampunya',
        403
      );
    }

    const updated = await prisma.attendanceSession.update({
      where: { id: sessionId },
      data: {
        status: SessionStatus.ditutup,
      },
      include: {
        schedule: {
          include: {
            class: true,
            subject: true,
            teacher: { select: { id: true, nama: true } },
            location: true,
          },
        },
      },
    });

    await prisma.auditLog.create({
      data: {
        actor_id: userId,
        action: 'SESSION_CLOSED',
        entity: 'attendance_sessions',
        entity_id: sessionId,
        old_value: { status: session.status },
        new_value: { status: SessionStatus.ditutup },
      },
    });

    return updated;
  }

  /**
   * Daftar sesi (terfilter sesuai role)
   */
  async getSessions(
    userId: string,
    userRole: string,
    query: {
      page: number;
      pageSize: number;
      status?: SessionStatus;
      tanggal?: string;
      class_id?: string;
      schedule_id?: string;
    }
  ) {
    const where: Prisma.AttendanceSessionWhereInput = {};

    if (query.status) {
      where.status = query.status;
    }
    if (query.tanggal) {
      where.tanggal = new Date(`${query.tanggal}T00:00:00.000Z`);
    }
    if (query.schedule_id) {
      where.schedule_id = query.schedule_id;
    }

    // Role-based boundary
    if (userRole === UserRole.operator) {
      if (query.class_id) {
        where.schedule = { class_id: query.class_id };
      }
    } else if (userRole === UserRole.guru) {
      where.schedule = {
        teacher_id: userId,
        ...(query.class_id ? { class_id: query.class_id } : {}),
      };
    } else if (userRole === UserRole.siswa) {
      // Siswa hanya melihat sesi untuk kelas yang dia adalah anggotanya
      where.schedule = {
        class: {
          members: {
            some: { user_id: userId },
          },
        },
        ...(query.class_id ? { class_id: query.class_id } : {}),
      };
    }

    const skip = (query.page - 1) * query.pageSize;
    const [total, data] = await Promise.all([
      prisma.attendanceSession.count({ where }),
      prisma.attendanceSession.findMany({
        where,
        skip,
        take: query.pageSize,
        orderBy: [{ tanggal: 'desc' }, { open_at: 'desc' }],
        include: {
          schedule: {
            include: {
              class: true,
              subject: true,
              teacher: { select: { id: true, nama: true, nomor_induk: true } },
              location: true,
            },
          },
          _count: {
            select: { attendances: true },
          },
        },
      }),
    ]);

    return {
      data,
      pagination: {
        page: query.page,
        pageSize: query.pageSize,
        total,
        totalPages: Math.ceil(total / query.pageSize) || 1,
      },
    };
  }

  /**
   * Buat challenge / nonce acak untuk siswa (TTL 2 menit)
   */
  async createChallenge(sessionId: string, studentId: string) {
    const session = await prisma.attendanceSession.findUnique({
      where: { id: sessionId },
      include: {
        schedule: {
          include: {
            class: {
              include: {
                members: { where: { user_id: studentId } },
              },
            },
          },
        },
      },
    });

    if (!session) {
      throw new AppError('NOT_FOUND', 'Sesi absensi tidak ditemukan', 404);
    }

    const now = new Date();

    // 1. Validasi status terbuka
    if (session.status !== SessionStatus.terbuka) {
      throw new AppError(
        'SESSION_CLOSED',
        'Sesi absensi tidak sedang terbuka',
        400
      );
    }

    // 2. Validasi rentang waktu server [open_at, close_at]
    if (now < session.open_at || now > session.close_at) {
      throw new AppError(
        'SESSION_CLOSED',
        'Waktu sesi absensi belum dibuka atau telah berakhir',
        400
      );
    }

    // 3. Validasi keanggotaan kelas
    if (!session.schedule.class.members || session.schedule.class.members.length === 0) {
      throw new AppError(
        'NOT_IN_CLASS',
        'Anda bukan siswa yang terdaftar pada kelas di sesi ini',
        403
      );
    }

    // Generate nonce kriptografi acak
    const nonce = crypto.randomBytes(32).toString('hex');
    const expiresAt = new Date(now.getTime() + 2 * 60 * 1000); // 2 menit TTL

    await prisma.attendanceChallenge.create({
      data: {
        user_id: studentId,
        session_id: sessionId,
        nonce,
        expires_at: expiresAt,
      },
    });

    return {
      nonce,
      expiresAt: expiresAt.toISOString(),
      serverTime: now.toISOString(),
    };
  }
}

export const sessionsService = new SessionsService();
