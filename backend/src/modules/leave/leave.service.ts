import path from 'path';
import { prisma } from '../../lib/prisma';
import { AppError } from '../../errors/appError';
import { env } from '../../config/env';
import { getStorageService } from '../../lib/storage';
import { AttendanceStatus, LeaveType, RequestStatus, UserRole, Prisma } from '@prisma/client';
import {
  CreateLeaveRequestInput,
  LeaveRequestQuery,
  ReviewLeaveRequestInput,
} from './leave.types';
import { fcmService } from '../../lib/fcm';

export class LeaveService {
  /**
   * Siswa: Ajukan izin / sakit untuk sesi tertentu
   */
  async createLeaveRequest(
    studentId: string,
    input: CreateLeaveRequestInput,
    file?: Express.Multer.File
  ) {
    const serverNow = new Date();

    // 1. Validasi sesi
    const session = await prisma.attendanceSession.findUnique({
      where: { id: input.session_id },
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
      throw new AppError('SESSION_NOT_FOUND', 'Sesi absensi tidak ditemukan', 404);
    }

    // 2. Pastikan siswa adalah anggota kelas pada jadwal tersebut
    if (!session.schedule.class.members || session.schedule.class.members.length === 0) {
      throw new AppError('NOT_IN_CLASS', 'Anda bukan siswa yang terdaftar di kelas sesi ini', 403);
    }

    // 3. Batas toleransi hari (backdate limit)
    const sessionDate = new Date(session.tanggal);
    const diffTime = serverNow.getTime() - sessionDate.getTime();
    const diffDays = diffTime / (1000 * 60 * 60 * 24);
    if (diffDays > env.LEAVE_MAX_BACKDATE_DAYS) {
      throw new AppError(
        'BACKDATE_LIMIT',
        `Pengajuan izin melebihi batas waktu maksimal ${env.LEAVE_MAX_BACKDATE_DAYS} hari ke belakang`,
        400
      );
    }

    // 4. Cek apakah sudah pernah check-in hadir/terlambat
    const existingAttendance = await prisma.attendance.findUnique({
      where: {
        session_id_student_id: {
          session_id: input.session_id,
          student_id: studentId,
        },
      },
    });

    if (
      existingAttendance &&
      (existingAttendance.status === AttendanceStatus.hadir ||
        existingAttendance.status === AttendanceStatus.terlambat)
    ) {
      throw new AppError(
        'ALREADY_CHECKED_IN',
        'Anda sudah melakukan absensi kehadiran pada sesi ini dan tidak dapat mengajukan izin',
        400
      );
    }

    // 5. Cek apakah sudah ada pengajuan izin yang berstatus 'menunggu' atau 'disetujui'
    const existingRequest = await prisma.leaveRequest.findFirst({
      where: {
        session_id: input.session_id,
        student_id: studentId,
        status: { in: [RequestStatus.menunggu, RequestStatus.disetujui] },
      },
    });

    if (existingRequest) {
      throw new AppError(
        'LEAVE_EXISTS',
        'Anda sudah memiliki pengajuan izin yang sedang diproses atau telah disetujui pada sesi ini',
        400
      );
    }

    // 6. Unggah lampiran jika disertakan
    let attachmentKey: string | null = null;
    if (file && file.buffer) {
      const ext = path.extname(file.originalname) || '.jpg';
      attachmentKey = `leave-attachments/${input.session_id}/${studentId}_${Date.now()}${ext}`;
      const storageService = getStorageService();
      await storageService.upload(attachmentKey, file.buffer, file.mimetype || 'image/jpeg');
    }

    // 7. Simpan pengajuan izin
    const leaveRequest = await prisma.leaveRequest.create({
      data: {
        student_id: studentId,
        session_id: input.session_id,
        tipe: input.tipe,
        alasan: input.alasan,
        attachment_key: attachmentKey,
        status: RequestStatus.menunggu,
      },
      include: {
        student: { select: { id: true, nama: true, nomor_induk: true } },
        session: {
          include: {
            schedule: {
              include: {
                class: true,
                subject: true,
                teacher: { select: { id: true, nama: true } },
              },
            },
          },
        },
      },
    });

    // 8. Catat log audit
    await prisma.auditLog.create({
      data: {
        actor_id: studentId,
        action: 'LEAVE_REQUEST_CREATED',
        entity: 'leave_requests',
        entity_id: leaveRequest.id,
        new_value: {
          session_id: input.session_id,
          tipe: input.tipe,
          alasan: input.alasan,
          attachment_key: attachmentKey,
        },
      },
    });

    return leaveRequest;
  }

  /**
   * Daftar izin terfilter per role (siswa, guru, operator)
   */
  async getLeaveRequests(
    user: { id: string; role: UserRole },
    query: LeaveRequestQuery
  ) {
    const page = query.page || 1;
    const pageSize = query.pageSize || 20;
    const skip = (page - 1) * pageSize;

    const where: Prisma.LeaveRequestWhereInput = {};

    if (query.status) {
      where.status = query.status;
    }
    if (query.tipe) {
      where.tipe = query.tipe;
    }
    if (query.session_id) {
      where.session_id = query.session_id;
    }

    // Filter berdasarkan role
    if (user.role === UserRole.siswa) {
      where.student_id = user.id;
    } else if (user.role === UserRole.guru) {
      where.session = {
        schedule: {
          teacher_id: user.id,
          ...(query.class_id ? { class_id: query.class_id } : {}),
        },
      };
    } else if (user.role === UserRole.operator) {
      if (query.student_id) {
        where.student_id = query.student_id;
      }
      if (query.class_id) {
        where.session = {
          schedule: {
            class_id: query.class_id,
          },
        };
      }
    }

    const [total, requests] = await Promise.all([
      prisma.leaveRequest.count({ where }),
      prisma.leaveRequest.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: { created_at: 'desc' },
        include: {
          student: { select: { id: true, nama: true, nomor_induk: true } },
          reviewer: { select: { id: true, nama: true } },
          session: {
            include: {
              schedule: {
                include: {
                  class: true,
                  subject: true,
                  teacher: { select: { id: true, nama: true } },
                },
              },
            },
          },
        },
      }),
    ]);

    return {
      data: requests,
      pagination: {
        page,
        pageSize,
        total,
        totalItems: total,
        totalPages: Math.ceil(total / pageSize) || 1,
      },
    };
  }

  /**
   * Guru / Operator: Setujui atau tolak izin
   */
  async reviewLeaveRequest(
    reviewerId: string,
    reviewerRole: UserRole,
    requestId: string,
    input: ReviewLeaveRequestInput
  ) {
    const serverNow = new Date();

    const leaveRequest = await prisma.leaveRequest.findUnique({
      where: { id: requestId },
      include: {
        session: {
          include: {
            schedule: true,
          },
        },
      },
    });

    if (!leaveRequest) {
      throw new AppError('NOT_FOUND', 'Pengajuan izin tidak ditemukan', 404);
    }

    // Role check: guru hanya bisa meninjau kelas yang diajarnya
    if (
      reviewerRole === UserRole.guru &&
      leaveRequest.session.schedule.teacher_id !== reviewerId
    ) {
      throw new AppError(
        'FORBIDDEN',
        'Guru hanya berwenang meninjau pengajuan izin pada mata pelajaran yang diajarnya',
        403
      );
    }

    if (leaveRequest.status !== RequestStatus.menunggu) {
      throw new AppError(
        'ALREADY_REVIEWED',
        `Pengajuan izin ini sudah diproses sebelumnya dengan status ${leaveRequest.status}`,
        400
      );
    }

    // Lakukan update leaveRequest dan rekonsiliasi attendances di dalam transaksi Prisma
    const updated = await prisma.$transaction(async (tx) => {
      const updatedLeave = await tx.leaveRequest.update({
        where: { id: requestId },
        data: {
          status: input.status,
          reviewed_by: reviewerId,
          reviewed_at: serverNow,
        },
        include: {
          student: { select: { id: true, nama: true, nomor_induk: true } },
          reviewer: { select: { id: true, nama: true } },
          session: {
            include: {
              schedule: {
                include: {
                  class: true,
                  subject: true,
                },
              },
            },
          },
        },
      });

      // Bila disetujui: ubah/buat status absensi menjadi izin atau sakit
      if (input.status === RequestStatus.disetujui) {
        const attendanceStatus =
          leaveRequest.tipe === LeaveType.sakit
            ? AttendanceStatus.sakit
            : AttendanceStatus.izin;

        const note = input.catatan
          ? `[Izin Disetujui] ${input.catatan}`
          : `[Izin Disetujui] ${leaveRequest.alasan}`;

        await tx.attendance.upsert({
          where: {
            session_id_student_id: {
              session_id: leaveRequest.session_id,
              student_id: leaveRequest.student_id,
            },
          },
          update: {
            status: attendanceStatus,
            catatan: note,
            source: 'izin',
          },
          create: {
            session_id: leaveRequest.session_id,
            student_id: leaveRequest.student_id,
            status: attendanceStatus,
            catatan: note,
            source: 'izin',
          },
        });
      }

      // Catat audit log
      await tx.auditLog.create({
        data: {
          actor_id: reviewerId,
          action:
            input.status === RequestStatus.disetujui
              ? 'LEAVE_REQUEST_APPROVED'
              : 'LEAVE_REQUEST_REJECTED',
          entity: 'leave_requests',
          entity_id: requestId,
          old_value: { status: leaveRequest.status },
          new_value: { status: input.status, catatan: input.catatan },
        },
      });

      return updatedLeave;
    });

    // Kirim notifikasi FCM ke siswa
    fcmService.notifyLeaveReviewed(requestId).catch(() => {});

    return updated;
  }

  /**
   * Mendapatkan signed URL lampiran surat izin
   */
  async getAttachmentSignedUrl(
    user: { id: string; role: UserRole },
    requestId: string
  ) {
    const leaveRequest = await prisma.leaveRequest.findUnique({
      where: { id: requestId },
      include: {
        session: {
          include: {
            schedule: true,
          },
        },
      },
    });

    if (!leaveRequest) {
      throw new AppError('NOT_FOUND', 'Pengajuan izin tidak ditemukan', 404);
    }

    if (!leaveRequest.attachment_key) {
      throw new AppError('ATTACHMENT_NOT_FOUND', 'Pengajuan izin ini tidak memiliki berkas lampiran', 404);
    }

    // Role check
    if (user.role === UserRole.siswa && leaveRequest.student_id !== user.id) {
      throw new AppError('FORBIDDEN', 'Anda tidak berhak melihat lampiran siswa lain', 403);
    }

    if (
      user.role === UserRole.guru &&
      leaveRequest.session.schedule.teacher_id !== user.id
    ) {
      throw new AppError(
        'FORBIDDEN',
        'Guru hanya dapat melihat lampiran izin pada mata pelajaran yang diajarnya',
        403
      );
    }

    const storageService = getStorageService();
    const signedUrl = await storageService.getSignedUrl(leaveRequest.attachment_key, 3600);

    return {
      url: signedUrl,
      expires_at: new Date(Date.now() + 3600 * 1000).toISOString(),
    };
  }
}
