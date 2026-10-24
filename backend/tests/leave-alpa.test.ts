import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from 'supertest';
import { app } from '../src/app';
import { prisma } from '../src/lib/prisma';
import { signAccessToken } from '../src/lib/token';
import {
  AttendanceStatus,
  LeaveType,
  RequestStatus,
  SessionStatus,
  UserRole,
} from '@prisma/client';
import { processAutoAlpa } from '../src/jobs/autoAlpa';

vi.mock('../src/lib/prisma', () => {
  return {
    prisma: {
      user: {
        findUnique: vi.fn(),
      },
      attendanceSession: {
        findUnique: vi.fn(),
        findMany: vi.fn(),
        updateMany: vi.fn(),
        update: vi.fn(),
      },
      leaveRequest: {
        findUnique: vi.fn(),
        findFirst: vi.fn(),
        findMany: vi.fn(),
        create: vi.fn(),
        update: vi.fn(),
        count: vi.fn(),
      },
      attendance: {
        findUnique: vi.fn(),
        findMany: vi.fn(),
        create: vi.fn(),
        createMany: vi.fn(),
        upsert: vi.fn(),
        update: vi.fn(),
      },
      auditLog: {
        create: vi.fn(),
      },
      $transaction: vi.fn((callback) => callback(prisma)),
    },
  };
});

describe('Leave Requests & Auto-Alpa (FR-IZN, FR-SES-03)', () => {
  const operatorId = '11111111-1111-1111-1111-111111111111';
  const guruId = '22222222-2222-2222-2222-222222222222';
  const otherGuruId = '33333333-3333-3333-3333-333333333333';
  const student1Id = '44444444-4444-4444-4444-444444444444';
  const student2Id = '55555555-5555-5555-5555-555555555555';
  const student3Id = '66666666-6666-6666-6666-666666666666';
  const nonMemberStudentId = '99999999-9999-9999-9999-999999999999';

  const classId = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
  const scheduleId = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
  const sessionId = 'cccccccc-cccc-cccc-cccc-cccccccccccc';
  const leaveRequestId = 'dddddddd-dddd-dddd-dddd-dddddddddddd';

  let operatorToken: string;
  let guruToken: string;
  let otherGuruToken: string;
  let student1Token: string;
  let nonMemberStudentToken: string;

  beforeEach(() => {
    vi.clearAllMocks();

    operatorToken = signAccessToken({ sub: operatorId, role: UserRole.operator });
    guruToken = signAccessToken({ sub: guruId, role: UserRole.guru });
    otherGuruToken = signAccessToken({ sub: otherGuruId, role: UserRole.guru });
    student1Token = signAccessToken({ sub: student1Id, role: UserRole.siswa });
    nonMemberStudentToken = signAccessToken({ sub: nonMemberStudentId, role: UserRole.siswa });

    // Mock User Authentication lookup
    vi.mocked(prisma.user.findUnique).mockImplementation(async ({ where }: any) => {
      const userMap: Record<string, any> = {
        [operatorId]: { id: operatorId, role: UserRole.operator, is_active: true },
        [guruId]: { id: guruId, role: UserRole.guru, is_active: true },
        [otherGuruId]: { id: otherGuruId, role: UserRole.guru, is_active: true },
        [student1Id]: { id: student1Id, role: UserRole.siswa, is_active: true },
        [student2Id]: { id: student2Id, role: UserRole.siswa, is_active: true },
        [student3Id]: { id: student3Id, role: UserRole.siswa, is_active: true },
        [nonMemberStudentId]: { id: nonMemberStudentId, role: UserRole.siswa, is_active: true },
      };
      return userMap[where.id];
    });
  });

  const setupValidSessionAndClass = (options: { isMember?: boolean } = { isMember: true }) => {
    vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue({
      id: sessionId,
      schedule_id: scheduleId,
      tanggal: new Date(),
      open_at: new Date(Date.now() - 30 * 60 * 1000),
      late_after: new Date(Date.now() - 10 * 60 * 1000),
      close_at: new Date(Date.now() + 60 * 60 * 1000),
      status: SessionStatus.terbuka,
      schedule: {
        id: scheduleId,
        class_id: classId,
        teacher_id: guruId,
        class: {
          id: classId,
          nama: 'XII MIPA 1',
          members: options.isMember ? [{ user_id: student1Id }] : [],
        },
      },
    } as any);

    vi.mocked(prisma.attendance.findUnique).mockResolvedValue(null);
    vi.mocked(prisma.leaveRequest.findFirst).mockResolvedValue(null);
  };

  describe('POST /api/v1/leave-requests (Siswa: Ajukan Izin/Sakit)', () => {
    it('should submit leave request with attachment successfully', async () => {
      setupValidSessionAndClass();
      vi.mocked(prisma.leaveRequest.create).mockResolvedValue({
        id: leaveRequestId,
        student_id: student1Id,
        session_id: sessionId,
        tipe: LeaveType.sakit,
        alasan: 'Demam tinggi dan istirahat dokter',
        attachment_key: 'leave-attachments/test.jpg',
        status: RequestStatus.menunggu,
      } as any);

      const res = await request(app)
        .post('/api/v1/leave-requests')
        .set('Authorization', `Bearer ${student1Token}`)
        .field('session_id', sessionId)
        .field('tipe', 'sakit')
        .field('alasan', 'Demam tinggi dan istirahat dokter')
        .attach('attachment', Buffer.from('fake-image-bytes'), 'surat_dokter.jpg');

      expect(res.status).toBe(201);
      expect(res.body.data.id).toBe(leaveRequestId);
      expect(res.body.data.tipe).toBe('sakit');
      expect(prisma.leaveRequest.create).toHaveBeenCalled();
      expect(prisma.auditLog.create).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({ action: 'LEAVE_REQUEST_CREATED' }),
        })
      );
    });

    it('should submit leave request without attachment successfully', async () => {
      setupValidSessionAndClass();
      vi.mocked(prisma.leaveRequest.create).mockResolvedValue({
        id: leaveRequestId,
        student_id: student1Id,
        session_id: sessionId,
        tipe: LeaveType.izin,
        alasan: 'Acara keluarga di luar kota',
        attachment_key: null,
        status: RequestStatus.menunggu,
      } as any);

      const res = await request(app)
        .post('/api/v1/leave-requests')
        .set('Authorization', `Bearer ${student1Token}`)
        .field('session_id', sessionId)
        .field('tipe', 'izin')
        .field('alasan', 'Acara keluarga di luar kota');

      expect(res.status).toBe(201);
      expect(res.body.data.tipe).toBe('izin');
    });

    it('should reject when student is not in class of the session', async () => {
      setupValidSessionAndClass({ isMember: false });

      const res = await request(app)
        .post('/api/v1/leave-requests')
        .set('Authorization', `Bearer ${nonMemberStudentToken}`)
        .field('session_id', sessionId)
        .field('tipe', 'izin')
        .field('alasan', 'Ada keperluan keluarga');

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('NOT_IN_CLASS');
    });

    it('should reject when student already attended this session', async () => {
      setupValidSessionAndClass();
      vi.mocked(prisma.attendance.findUnique).mockResolvedValue({
        id: 'att-1',
        session_id: sessionId,
        student_id: student1Id,
        status: AttendanceStatus.hadir,
      } as any);

      const res = await request(app)
        .post('/api/v1/leave-requests')
        .set('Authorization', `Bearer ${student1Token}`)
        .field('session_id', sessionId)
        .field('tipe', 'izin')
        .field('alasan', 'Ada keperluan');

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('ALREADY_CHECKED_IN');
    });

    it('should reject when student already has a pending leave request', async () => {
      setupValidSessionAndClass();
      vi.mocked(prisma.leaveRequest.findFirst).mockResolvedValue({
        id: 'existing-leave-id',
        status: RequestStatus.menunggu,
      } as any);

      const res = await request(app)
        .post('/api/v1/leave-requests')
        .set('Authorization', `Bearer ${student1Token}`)
        .field('session_id', sessionId)
        .field('tipe', 'sakit')
        .field('alasan', 'Sakit flu');

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('LEAVE_EXISTS');
    });
  });

  describe('GET /api/v1/leave-requests (Role-filtered List)', () => {
    it('Siswa: should only receive own leave requests', async () => {
      vi.mocked(prisma.leaveRequest.count).mockResolvedValue(1);
      vi.mocked(prisma.leaveRequest.findMany).mockResolvedValue([
        { id: leaveRequestId, student_id: student1Id, tipe: LeaveType.sakit },
      ] as any);

      const res = await request(app)
        .get('/api/v1/leave-requests')
        .set('Authorization', `Bearer ${student1Token}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(1);
      expect(prisma.leaveRequest.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: expect.objectContaining({ student_id: student1Id }),
        })
      );
    });

    it('Guru: should only receive leave requests for their taught classes', async () => {
      vi.mocked(prisma.leaveRequest.count).mockResolvedValue(2);
      vi.mocked(prisma.leaveRequest.findMany).mockResolvedValue([
        { id: 'l-1', session: { schedule: { teacher_id: guruId } } },
        { id: 'l-2', session: { schedule: { teacher_id: guruId } } },
      ] as any);

      const res = await request(app)
        .get('/api/v1/leave-requests')
        .set('Authorization', `Bearer ${guruToken}`);

      expect(res.status).toBe(200);
      expect(prisma.leaveRequest.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: expect.objectContaining({
            session: expect.objectContaining({
              schedule: expect.objectContaining({ teacher_id: guruId }),
            }),
          }),
        })
      );
    });
  });

  describe('PATCH /api/v1/leave-requests/:id (Persetujuan Izin & Rekonsiliasi Absen)', () => {
    const mockLeaveRequest = {
      id: leaveRequestId,
      student_id: student1Id,
      session_id: sessionId,
      tipe: LeaveType.sakit,
      alasan: 'Sakit tifus',
      status: RequestStatus.menunggu,
      session: {
        id: sessionId,
        schedule: {
          id: scheduleId,
          teacher_id: guruId,
          class_id: classId,
        },
      },
    };

    it('Guru: should successfully approve leave request and update attendance to SAKIT', async () => {
      vi.mocked(prisma.leaveRequest.findUnique).mockResolvedValue(mockLeaveRequest as any);
      vi.mocked(prisma.leaveRequest.update).mockResolvedValue({
        ...mockLeaveRequest,
        status: RequestStatus.disetujui,
      } as any);

      const res = await request(app)
        .patch(`/api/v1/leave-requests/${leaveRequestId}`)
        .set('Authorization', `Bearer ${guruToken}`)
        .send({ status: 'disetujui', catatan: 'Semoga lekas sembuh' });

      expect(res.status).toBe(200);
      expect(res.body.data.status).toBe('disetujui');
      expect(prisma.attendance.upsert).toHaveBeenCalledWith(
        expect.objectContaining({
          create: expect.objectContaining({
            session_id: sessionId,
            student_id: student1Id,
            status: AttendanceStatus.sakit,
          }),
        })
      );
      expect(prisma.auditLog.create).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({ action: 'LEAVE_REQUEST_APPROVED' }),
        })
      );
    });

    it('Guru: should reject approval attempt by teacher of another class', async () => {
      vi.mocked(prisma.leaveRequest.findUnique).mockResolvedValue(mockLeaveRequest as any);

      const res = await request(app)
        .patch(`/api/v1/leave-requests/${leaveRequestId}`)
        .set('Authorization', `Bearer ${otherGuruToken}`)
        .send({ status: 'disetujui' });

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    it('Operator: can reject leave request', async () => {
      vi.mocked(prisma.leaveRequest.findUnique).mockResolvedValue(mockLeaveRequest as any);
      vi.mocked(prisma.leaveRequest.update).mockResolvedValue({
        ...mockLeaveRequest,
        status: RequestStatus.ditolak,
      } as any);

      const res = await request(app)
        .patch(`/api/v1/leave-requests/${leaveRequestId}`)
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({ status: 'ditolak', catatan: 'Surat tidak lengkap' });

      expect(res.status).toBe(200);
      expect(res.body.data.status).toBe('ditolak');
      expect(prisma.auditLog.create).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({ action: 'LEAVE_REQUEST_REJECTED' }),
        })
      );
    });

    it('should reject review when leave request is already approved/rejected', async () => {
      vi.mocked(prisma.leaveRequest.findUnique).mockResolvedValue({
        ...mockLeaveRequest,
        status: RequestStatus.disetujui, // already reviewed
      } as any);

      const res = await request(app)
        .patch(`/api/v1/leave-requests/${leaveRequestId}`)
        .set('Authorization', `Bearer ${guruToken}`)
        .send({ status: 'disetujui' });

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('ALREADY_REVIEWED');
    });
  });

  describe('GET /api/v1/leave-requests/:id/attachment (Signed URL Lampiran)', () => {
    it('Siswa: should get signed URL for own leave request attachment', async () => {
      vi.mocked(prisma.leaveRequest.findUnique).mockResolvedValue({
        id: leaveRequestId,
        student_id: student1Id,
        attachment_key: 'leave-attachments/test.jpg',
        session: { schedule: { teacher_id: guruId } },
      } as any);

      const res = await request(app)
        .get(`/api/v1/leave-requests/${leaveRequestId}/attachment`)
        .set('Authorization', `Bearer ${student1Token}`);

      expect(res.status).toBe(200);
      expect(res.body.data.url).toBeDefined();
      expect(res.body.data.expires_at).toBeDefined();
    });

    it('Siswa: should be forbidden from accessing other student attachment', async () => {
      vi.mocked(prisma.leaveRequest.findUnique).mockResolvedValue({
        id: leaveRequestId,
        student_id: student1Id,
        attachment_key: 'leave-attachments/test.jpg',
        session: { schedule: { teacher_id: guruId } },
      } as any);

      const res = await request(app)
        .get(`/api/v1/leave-requests/${leaveRequestId}/attachment`)
        .set('Authorization', `Bearer ${nonMemberStudentToken}`);

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });
  });

  describe('Auto-Alpa Job (FR-SES-03: processAutoAlpa)', () => {
    it('should set unattended students to ALPA and reconcile approved leave when session is closed', async () => {
      // Session with 3 class members:
      // student1: checked in (hadir)
      // student2: approved leave (sakit)
      // student3: neither checked in nor approved leave -> should become ALPA!
      vi.mocked(prisma.attendanceSession.findMany).mockResolvedValue([
        {
          id: sessionId,
          status: SessionStatus.ditutup,
          schedule: {
            class: {
              members: [
                { user_id: student1Id },
                { user_id: student2Id },
                { user_id: student3Id },
              ],
            },
          },
          attendances: [
            { student_id: student1Id }, // already checked in
          ],
          leave_requests: [
            { student_id: student2Id, tipe: LeaveType.sakit, alasan: 'Sakit gigi' },
          ],
        },
      ] as any);

      const result = await processAutoAlpa(sessionId);

      expect(result.processedSessions).toBe(1);
      expect(result.alpaCreated).toBe(1); // student3 becomes alpa
      expect(result.leaveReconciled).toBe(1); // student2 becomes sakit

      expect(prisma.attendance.createMany).toHaveBeenCalledWith({
        data: expect.arrayContaining([
          expect.objectContaining({
            session_id: sessionId,
            student_id: student3Id,
            status: AttendanceStatus.alpa,
            source: 'system',
          }),
          expect.objectContaining({
            session_id: sessionId,
            student_id: student2Id,
            status: AttendanceStatus.sakit,
            source: 'izin',
          }),
        ]),
        skipDuplicates: true,
      });
    });

    it('should be idempotent and not create duplicate attendances when run again', async () => {
      // All 3 students now already have attendances
      vi.mocked(prisma.attendanceSession.findMany).mockResolvedValue([
        {
          id: sessionId,
          status: SessionStatus.ditutup,
          schedule: {
            class: {
              members: [
                { user_id: student1Id },
                { user_id: student2Id },
                { user_id: student3Id },
              ],
            },
          },
          attendances: [
            { student_id: student1Id },
            { student_id: student2Id },
            { student_id: student3Id },
          ],
          leave_requests: [],
        },
      ] as any);

      const result = await processAutoAlpa(sessionId);

      expect(result.processedSessions).toBe(1);
      expect(result.alpaCreated).toBe(0);
      expect(result.leaveReconciled).toBe(0);
      expect(prisma.attendance.createMany).not.toHaveBeenCalled();
    });
  });
});
