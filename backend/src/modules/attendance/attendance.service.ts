import sharp from 'sharp';
import { prisma } from '../../lib/prisma';
import { AppError } from '../../errors/appError';
import { AttendanceStatus, SessionStatus, UserRole, Prisma } from '@prisma/client';
import { calculateHaversineDistance } from '../../lib/haversine';
import { getStorageService } from '../../lib/storage';
import { getIntegrityVerifier } from '../../lib/integrity';
import { logger } from '../../lib/logger';
import { env } from '../../config/env';

export class AttendanceService {
  private async logRejection(
    sessionId: string,
    studentId: string,
    code: string,
    message: string,
    details?: Record<string, unknown>
  ) {
    try {
      await prisma.auditLog.create({
        data: {
          actor_id: studentId,
          action: 'ATTENDANCE_REJECTED',
          entity: 'attendance_sessions',
          entity_id: sessionId,
          new_value: {
            code,
            message,
            student_id: studentId,
            session_id: sessionId,
            ...details,
          },
        },
      });
    } catch (err) {
      logger.error({ err }, 'Failed to record rejection audit log');
    }
  }

  /**
   * Submit Absensi Siswa (Multipart)
   * Validasi berurutan sesuai Langkah 1-12 Bagian 6 RND.md
   */
  async submitAttendance(
    studentId: string,
    sessionId: string,
    data: {
      lat: number;
      lng: number;
      accuracy: number;
      nonce: string;
      device_id: string;
      mock_flag: boolean;
      integrity_token?: string;
    },
    file?: Express.Multer.File
  ) {
    const serverNow = new Date();

    // 1. JWT valid dan role = siswa (sudah divalidasi oleh middleware requireRole)

    // 2. Sesi ada, status 'terbuka', dan waktu server dalam [open_at, close_at]
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
            location: true,
          },
        },
      },
    });

    if (!session || session.status !== SessionStatus.terbuka) {
      await this.logRejection(
        sessionId,
        studentId,
        'SESSION_CLOSED',
        'Sesi absensi tidak dalam status terbuka'
      );
      throw new AppError(
        'SESSION_CLOSED',
        'Sesi absensi tidak sedang terbuka',
        400
      );
    }

    if (serverNow < session.open_at || serverNow > session.close_at) {
      await this.logRejection(
        sessionId,
        studentId,
        'SESSION_CLOSED',
        'Waktu absensi belum dibuka atau telah berakhir',
        { open_at: session.open_at, close_at: session.close_at, server_time: serverNow }
      );
      throw new AppError(
        'SESSION_CLOSED',
        'Waktu sesi absensi belum dibuka atau telah berakhir',
        400
      );
    }

    // 3. Siswa anggota kelas pada jadwal sesi tersebut
    if (!session.schedule.class.members || session.schedule.class.members.length === 0) {
      await this.logRejection(
        sessionId,
        studentId,
        'NOT_IN_CLASS',
        'Siswa bukan anggota kelas sesi ini'
      );
      throw new AppError(
        'NOT_IN_CLASS',
        'Anda bukan siswa yang terdaftar di kelas sesi ini',
        403
      );
    }

    // 4. device_id sama dengan perangkat aktif siswa yang disetujui
    const activeDevice = await prisma.device.findFirst({
      where: {
        user_id: studentId,
        device_id: data.device_id,
        is_active: true,
        status: 'disetujui',
      },
    });

    if (!activeDevice) {
      await this.logRejection(
        sessionId,
        studentId,
        'DEVICE_MISMATCH',
        'Perangkat tidak cocok atau belum disetujui',
        { device_id: data.device_id }
      );
      throw new AppError(
        'DEVICE_MISMATCH',
        'Absensi hanya dapat dilakukan dari perangkat resmi yang telah disetujui operator',
        403
      );
    }

    // 5. Cek apakah siswa sudah pernah absen pada sesi ini (unique session_id + student_id)
    const existingAttendance = await prisma.attendance.findUnique({
      where: {
        session_id_student_id: {
          session_id: sessionId,
          student_id: studentId,
        },
      },
    });

    if (existingAttendance) {
      await this.logRejection(
        sessionId,
        studentId,
        'ALREADY_CHECKED_IN',
        'Siswa sudah melakukan absensi pada sesi ini'
      );
      throw new AppError(
        'ALREADY_CHECKED_IN',
        'Anda sudah melakukan absensi pada sesi ini',
        409
      );
    }

    // 6. Nonce valid, belum dipakai, belum kedaluwarsa. Verifikasi Play Integrity
    const challenge = await prisma.attendanceChallenge.findUnique({
      where: { nonce: data.nonce },
    });

    if (
      !challenge ||
      challenge.session_id !== sessionId ||
      challenge.user_id !== studentId ||
      challenge.used_at !== null ||
      challenge.expires_at < serverNow
    ) {
      await this.logRejection(
        sessionId,
        studentId,
        'NONCE_INVALID',
        'Nonce challenge tidak valid, sudah dipakai, atau kedaluwarsa'
      );
      throw new AppError(
        'NONCE_INVALID',
        'Token challenge tidak valid atau telah kedaluwarsa. Silakan muat ulang layar absen.',
        400
      );
    }

    const integrityVerifier = getIntegrityVerifier();
    const integrityResult = await integrityVerifier.verify(
      data.integrity_token,
      data.nonce
    );
    if (!integrityResult.isValid) {
      await this.logRejection(
        sessionId,
        studentId,
        'INTEGRITY_FAILED',
        integrityResult.errorDetails || 'Verifikasi integritas perangkat gagal',
        { verdict: integrityResult.verdict, details: integrityResult.errorDetails }
      );
      throw new AppError(
        'INTEGRITY_FAILED',
        'Verifikasi keaslian perangkat dan aplikasi gagal',
        400
      );
    }

    // 7. Tolak bila flag mock location dari klien = true
    if (data.mock_flag) {
      await this.logRejection(
        sessionId,
        studentId,
        'MOCK_LOCATION',
        'Mock location / Fake GPS terdeteksi aktif'
      );
      throw new AppError(
        'MOCK_LOCATION',
        'Penggunaan aplikasi lokasi palsu (Fake GPS) dilarang dan terdeteksi oleh sistem',
        400
      );
    }

    // 8. Akurasi GPS <= batas (default 50 m)
    const maxAccuracy = env.ATTENDANCE_MAX_ACCURACY_M;
    if (data.accuracy > maxAccuracy) {
      await this.logRejection(
        sessionId,
        studentId,
        'LOW_ACCURACY',
        `Akurasi GPS ${data.accuracy}m melebihi batas ${maxAccuracy}m`,
        { accuracy: data.accuracy, max_accuracy: maxAccuracy }
      );
      throw new AppError(
        'LOW_ACCURACY',
        `Akurasi sinyal GPS terlalu rendah (lebih dari ${maxAccuracy} meter). Pindahlah ke area terbuka.`,
        400
      );
    }

    // 9. Hitung jarak Haversine ke lokasi sesi
    const locLat = Number(session.schedule.location.latitude);
    const locLng = Number(session.schedule.location.longitude);
    const radiusM = session.schedule.location.radius_m;

    const distanceM = calculateHaversineDistance(
      data.lat,
      data.lng,
      locLat,
      locLng
    );

    if (distanceM > radiusM) {
      await this.logRejection(
        sessionId,
        studentId,
        'OUT_OF_RADIUS',
        `Jarak ${Math.round(distanceM)}m melebihi radius ${radiusM}m`,
        { distance_m: distanceM, radius_m: radiusM, lat: data.lat, lng: data.lng }
      );
      throw new AppError(
        'OUT_OF_RADIUS',
        `Anda berada di luar radius presensi (${Math.round(distanceM)} meter dari lokasi, radius maksimal ${radiusM} meter)`,
        400
      );
    }

    // 10. Validasi selfie: tipe JPEG, ukuran <= 2 MB, kompres sharp & strip EXIF
    if (!file || !file.buffer) {
      await this.logRejection(
        sessionId,
        studentId,
        'INVALID_SELFIE',
        'File foto selfie tidak ditemukan'
      );
      throw new AppError('INVALID_SELFIE', 'Foto selfie wajib dikirimkan', 400);
    }

    const isJpeg =
      file.mimetype === 'image/jpeg' ||
      file.mimetype === 'image/jpg' ||
      file.originalname.toLowerCase().endsWith('.jpg') ||
      file.originalname.toLowerCase().endsWith('.jpeg');

    if (!isJpeg) {
      await this.logRejection(
        sessionId,
        studentId,
        'INVALID_SELFIE',
        `Mime type ${file.mimetype} bukan image/jpeg`
      );
      throw new AppError('INVALID_SELFIE', 'Foto selfie harus berformat JPEG', 400);
    }

    if (file.buffer.length > 2 * 1024 * 1024) {
      await this.logRejection(
        sessionId,
        studentId,
        'INVALID_SELFIE',
        `Ukuran file ${file.buffer.length} melebihi 2 MB`
      );
      throw new AppError(
        'INVALID_SELFIE',
        'Ukuran file foto selfie melebihi batas 2 MB',
        400
      );
    }

    let processedBuffer: Buffer;
    try {
      processedBuffer = await sharp(file.buffer)
        .rotate()
        .resize({
          width: 1280,
          height: 1280,
          fit: 'inside',
          withoutEnlargement: true,
        })
        .jpeg({ quality: 80 })
        .toBuffer();
    } catch (err) {
      await this.logRejection(
        sessionId,
        studentId,
        'INVALID_SELFIE',
        'Gagal memproses gambar JPEG'
      );
      throw new AppError(
        'INVALID_SELFIE',
        'File foto selfie tidak valid atau korup',
        400
      );
    }

    // 11. Tentukan status: 'hadir' bila waktu server <= late_after, selain itu 'terlambat'
    const status: AttendanceStatus =
      serverNow <= session.late_after
        ? AttendanceStatus.hadir
        : AttendanceStatus.terlambat;

    // 12. Simpan ke storage privat dan catat di database
    const storageService = getStorageService();
    const objectKey = `selfies/${sessionId}/${studentId}.jpg`;
    await storageService.upload(objectKey, processedBuffer, 'image/jpeg');

    // Tandai challenge sudah digunakan
    await prisma.attendanceChallenge.update({
      where: { id: challenge.id },
      data: { used_at: serverNow },
    });

    // Simpan data absensi
    const attendance = await prisma.attendance.create({
      data: {
        session_id: sessionId,
        student_id: studentId,
        device_id: activeDevice.id,
        status,
        checkin_at: serverNow,
        latitude: data.lat,
        longitude: data.lng,
        accuracy_m: data.accuracy,
        distance_m: Math.round(distanceM * 100) / 100,
        selfie_key: objectKey,
        mock_detected: false,
        integrity_verdict: integrityResult.verdict,
        source: 'app',
      },
      include: {
        session: {
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
        },
      },
    });

    // Audit log absensi sukses
    await prisma.auditLog.create({
      data: {
        actor_id: studentId,
        action: 'ATTENDANCE_RECORDED',
        entity: 'attendances',
        entity_id: attendance.id,
        new_value: {
          session_id: sessionId,
          status,
          distance_m: attendance.distance_m,
          checkin_at: serverNow,
        },
      },
    });

    return attendance;
  }

  /**
   * Riwayat kehadiran siswa yang login (paginasi)
   */
  async getMyAttendances(
    studentId: string,
    query: {
      page: number;
      pageSize: number;
      status?: AttendanceStatus;
      from_date?: string;
      to_date?: string;
    }
  ) {
    const where: Prisma.AttendanceWhereInput = {
      student_id: studentId,
    };

    if (query.status) {
      where.status = query.status;
    }

    if (query.from_date || query.to_date) {
      const tanggalFilter: Prisma.DateTimeFilter = {};
      if (query.from_date) {
        tanggalFilter.gte = new Date(`${query.from_date}T00:00:00.000Z`);
      }
      if (query.to_date) {
        tanggalFilter.lte = new Date(`${query.to_date}T00:00:00.000Z`);
      }
      where.session = { tanggal: tanggalFilter };
    }

    const skip = (query.page - 1) * query.pageSize;
    const [total, data] = await Promise.all([
      prisma.attendance.count({ where }),
      prisma.attendance.findMany({
        where,
        skip,
        take: query.pageSize,
        orderBy: { checkin_at: 'desc' },
        include: {
          session: {
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
   * Rekap absensi (operator melihat semua, guru hanya kelas yang diampunya)
   */
  async getAttendances(
    userId: string,
    userRole: string,
    query: {
      page: number;
      pageSize: number;
      session_id?: string;
      class_id?: string;
      subject_id?: string;
      status?: AttendanceStatus;
      tanggal?: string;
    }
  ) {
    const where: Prisma.AttendanceWhereInput = {};

    if (query.session_id) {
      where.session_id = query.session_id;
    }
    if (query.status) {
      where.status = query.status;
    }

    const sessionWhere: Prisma.AttendanceSessionWhereInput = {};
    if (query.tanggal) {
      sessionWhere.tanggal = new Date(`${query.tanggal}T00:00:00.000Z`);
    }

    const scheduleWhere: Prisma.ScheduleWhereInput = {};
    if (query.class_id) {
      scheduleWhere.class_id = query.class_id;
    }
    if (query.subject_id) {
      scheduleWhere.subject_id = query.subject_id;
    }

    // Role constraint: Guru hanya kelas yang dia ampu
    if (userRole === UserRole.guru) {
      if (query.class_id) {
        const teachesClass = await prisma.schedule.findFirst({
          where: {
            teacher_id: userId,
            class_id: query.class_id,
          },
        });
        if (!teachesClass) {
          throw new AppError(
            'FORBIDDEN',
            'Guru hanya dapat melihat rekap absensi untuk kelas yang diampunya',
            403
          );
        }
      }
      scheduleWhere.teacher_id = userId;
    }

    if (Object.keys(scheduleWhere).length > 0) {
      sessionWhere.schedule = scheduleWhere;
    }
    if (Object.keys(sessionWhere).length > 0) {
      where.session = sessionWhere;
    }

    const skip = (query.page - 1) * query.pageSize;
    const [total, data] = await Promise.all([
      prisma.attendance.count({ where }),
      prisma.attendance.findMany({
        where,
        skip,
        take: query.pageSize,
        orderBy: { checkin_at: 'desc' },
        include: {
          student: {
            select: {
              id: true,
              nama: true,
              nomor_induk: true,
              email: true,
            },
          },
          session: {
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
   * Koreksi manual status absensi (catatan wajib, audit log old/new)
   */
  async correctAttendance(
    attendanceId: string,
    userId: string,
    userRole: string,
    data: {
      status: AttendanceStatus;
      catatan: string;
    }
  ) {
    const attendance = await prisma.attendance.findUnique({
      where: { id: attendanceId },
      include: {
        session: {
          include: {
            schedule: true,
          },
        },
      },
    });

    if (!attendance) {
      throw new AppError('NOT_FOUND', 'Data absensi tidak ditemukan', 404);
    }

    // Guru hanya boleh koreksi absensi kelas yang diampunya
    if (
      userRole === UserRole.guru &&
      attendance.session.schedule.teacher_id !== userId
    ) {
      throw new AppError(
        'FORBIDDEN',
        'Guru hanya dapat mengoreksi absensi untuk jadwal kelas yang diampunya',
        403
      );
    }

    const updated = await prisma.attendance.update({
      where: { id: attendanceId },
      data: {
        status: data.status,
        catatan: data.catatan,
        source: 'manual',
      },
      include: {
        student: { select: { id: true, nama: true, nomor_induk: true } },
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

    // Wajib catat audit log old/new
    await prisma.auditLog.create({
      data: {
        actor_id: userId,
        action: 'ATTENDANCE_CORRECTED',
        entity: 'attendances',
        entity_id: attendanceId,
        old_value: {
          status: attendance.status,
          catatan: attendance.catatan,
          source: attendance.source,
        },
        new_value: {
          status: data.status,
          catatan: data.catatan,
          source: 'manual',
        },
      },
    });

    return updated;
  }

  /**
   * Ambil signed URL selfie berumur pendek
   */
  async getSelfieSignedUrl(attendanceId: string, userId: string, userRole: string) {
    const attendance = await prisma.attendance.findUnique({
      where: { id: attendanceId },
      include: {
        session: {
          include: {
            schedule: true,
          },
        },
      },
    });

    if (!attendance) {
      throw new AppError('NOT_FOUND', 'Data absensi tidak ditemukan', 404);
    }

    if (!attendance.selfie_key) {
      throw new AppError(
        'NOT_FOUND',
        'Foto selfie tidak ditemukan untuk absensi ini',
        404
      );
    }

    // Hak akses: Operator, Guru pengampu, atau Siswa pemilik absensi
    const isOwner = attendance.student_id === userId;
    const isTeacher =
      userRole === UserRole.guru &&
      attendance.session.schedule.teacher_id === userId;
    const isOperator = userRole === UserRole.operator;

    if (!isOwner && !isTeacher && !isOperator) {
      throw new AppError(
        'FORBIDDEN',
        'Anda tidak memiliki izin untuk melihat foto selfie ini',
        403
      );
    }

    const storage = getStorageService();
    const signedUrl = await storage.getSignedUrl(attendance.selfie_key, 900); // 15 menit

    return {
      url: signedUrl,
      expires_in: 900,
    };
  }
}

export const attendanceService = new AttendanceService();
