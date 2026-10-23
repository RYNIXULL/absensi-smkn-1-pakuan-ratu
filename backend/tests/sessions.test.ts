import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from 'supertest';
import { app } from '../src/app';
import { prisma } from '../src/lib/prisma';
import { signAccessToken } from '../src/lib/token';
import { processSessionLifecycle } from '../src/jobs/sessionManager';
import { SessionStatus } from '@prisma/client';

vi.mock('../src/lib/prisma', () => {
  return {
    prisma: {
      user: {
        findUnique: vi.fn(),
      },
      schedule: {
        findMany: vi.fn(),
        findUnique: vi.fn(),
      },
      attendanceSession: {
        findMany: vi.fn(),
        findUnique: vi.fn(),
        create: vi.fn(),
        createMany: vi.fn(),
        update: vi.fn(),
        updateMany: vi.fn(),
        count: vi.fn(),
      },
      attendanceChallenge: {
        create: vi.fn(),
      },
      auditLog: {
        create: vi.fn(),
      },
    },
  };
});

describe('Sessions & Cron Lifecycle Tests (FR-SES)', () => {
  const operatorId = '00000000-0000-0000-0000-000000000001';
  const guruClassOwnerId = '22222222-2222-2222-2222-222222222222';
  const guruOtherId = '33333333-3333-3333-3333-333333333333';
  const siswaId = '44444444-4444-4444-4444-444444444444';
  const siswaOtherId = '55555555-5555-5555-5555-555555555555';

  const operatorToken = signAccessToken({ sub: operatorId, role: 'operator' });
  const guruOwnerToken = signAccessToken({ sub: guruClassOwnerId, role: 'guru' });
  const guruOtherToken = signAccessToken({ sub: guruOtherId, role: 'guru' });
  const siswaToken = signAccessToken({ sub: siswaId, role: 'siswa' });
  const siswaOtherToken = signAccessToken({ sub: siswaOtherId, role: 'siswa' });

  const scheduleId = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
  const sessionId = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(prisma.user.findUnique).mockImplementation(async ({ where }) => {
      const id = where.id;
      if (id === operatorId) return { id: operatorId, role: 'operator', is_active: true } as any;
      if (id === guruClassOwnerId) return { id: guruClassOwnerId, role: 'guru', is_active: true } as any;
      if (id === guruOtherId) return { id: guruOtherId, role: 'guru', is_active: true } as any;
      if (id === siswaId) return { id: siswaId, role: 'siswa', is_active: true } as any;
      if (id === siswaOtherId) return { id: siswaOtherId, role: 'siswa', is_active: true } as any;
      return null;
    });
  });

  describe('Session Lifecycle Cron Job (processSessionLifecycle)', () => {
    it('should create scheduled sessions and remain idempotent on consecutive runs', async () => {
      const mockSchedule = {
        id: scheduleId,
        class_id: 'class-1',
        subject_id: 'sub-1',
        teacher_id: guruClassOwnerId,
        location_id: 'loc-1',
        day_of_week: 5,
        start_time: new Date('1970-01-01T07:30:00.000Z'),
        end_time: new Date('1970-01-01T09:00:00.000Z'),
      };

      // Run 1: No sessions exist yet for today
      vi.mocked(prisma.schedule.findMany).mockResolvedValue([mockSchedule] as any);
      vi.mocked(prisma.attendanceSession.findMany).mockResolvedValue([] as any);
      vi.mocked(prisma.attendanceSession.createMany).mockResolvedValue({ count: 1 });
      vi.mocked(prisma.attendanceSession.updateMany).mockResolvedValue({ count: 0 });

      const run1 = await processSessionLifecycle(new Date('2026-10-23T06:00:00.000Z'));
      expect(run1.createdSessions).toBe(1);
      expect(prisma.attendanceSession.createMany).toHaveBeenCalledTimes(1);

      // Run 2: Session for today already exists in DB -> Idempotent, 0 new sessions created
      vi.mocked(prisma.attendanceSession.findMany).mockResolvedValue([
        { schedule_id: scheduleId },
      ] as any);

      const run2 = await processSessionLifecycle(new Date('2026-10-23T06:05:00.000Z'));
      expect(run2.createdSessions).toBe(0);
    });

    it('should transition status from terjadwal to terbuka when open_at is reached', async () => {
      vi.mocked(prisma.schedule.findMany).mockResolvedValue([]);
      vi.mocked(prisma.attendanceSession.findMany).mockResolvedValue([]);
      vi.mocked(prisma.attendanceSession.updateMany)
        .mockResolvedValueOnce({ count: 2 }) // opened
        .mockResolvedValueOnce({ count: 0 }); // closed

      const result = await processSessionLifecycle(new Date());
      expect(result.openedSessions).toBe(2);
      expect(prisma.attendanceSession.updateMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: expect.objectContaining({
            status: SessionStatus.terjadwal,
          }),
          data: { status: SessionStatus.terbuka },
        })
      );
    });
  });

  describe('POST /api/v1/sessions (Buka Sesi Manual)', () => {
    it('Operator: should successfully open manual session', async () => {
      vi.mocked(prisma.schedule.findUnique).mockResolvedValue({
        id: scheduleId,
        teacher_id: guruClassOwnerId,
        start_time: new Date('1970-01-01T07:30:00.000Z'),
        end_time: new Date('1970-01-01T09:00:00.000Z'),
      } as any);

      vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(null);
      vi.mocked(prisma.attendanceSession.create).mockResolvedValue({
        id: sessionId,
        schedule_id: scheduleId,
        status: SessionStatus.terbuka,
        opened_by: operatorId,
      } as any);

      const res = await request(app)
        .post('/api/v1/sessions')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({
          schedule_id: scheduleId,
          tanggal: '2026-10-23',
        });

      expect(res.status).toBe(201);
      expect(res.body.data.status).toBe('terbuka');
      expect(prisma.auditLog.create).toHaveBeenCalled();
    });

    it('Guru: should open manual session for own schedule', async () => {
      vi.mocked(prisma.schedule.findUnique).mockResolvedValue({
        id: scheduleId,
        teacher_id: guruClassOwnerId,
        start_time: new Date('1970-01-01T07:30:00.000Z'),
        end_time: new Date('1970-01-01T09:00:00.000Z'),
      } as any);

      vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(null);
      vi.mocked(prisma.attendanceSession.create).mockResolvedValue({
        id: sessionId,
        schedule_id: scheduleId,
        status: SessionStatus.terbuka,
        opened_by: guruClassOwnerId,
      } as any);

      const res = await request(app)
        .post('/api/v1/sessions')
        .set('Authorization', `Bearer ${guruOwnerToken}`)
        .send({ schedule_id: scheduleId });

      expect(res.status).toBe(201);
      expect(res.body.data.status).toBe('terbuka');
    });

    it('Guru: should reject opening session for another teacher schedule (403 FORBIDDEN)', async () => {
      vi.mocked(prisma.schedule.findUnique).mockResolvedValue({
        id: scheduleId,
        teacher_id: guruClassOwnerId, // Owner is guruClassOwnerId, caller is guruOtherId
        start_time: new Date('1970-01-01T07:30:00.000Z'),
        end_time: new Date('1970-01-01T09:00:00.000Z'),
      } as any);

      const res = await request(app)
        .post('/api/v1/sessions')
        .set('Authorization', `Bearer ${guruOtherToken}`)
        .send({ schedule_id: scheduleId });

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    it('Siswa: should be forbidden to open session (403 FORBIDDEN)', async () => {
      const res = await request(app)
        .post('/api/v1/sessions')
        .set('Authorization', `Bearer ${siswaToken}`)
        .send({ schedule_id: scheduleId });

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });
  });

  describe('PATCH /api/v1/sessions/:id/close (Tutup Sesi)', () => {
    it('Operator: should close active session and log audit', async () => {
      vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue({
        id: sessionId,
        status: SessionStatus.terbuka,
        schedule: { teacher_id: guruClassOwnerId },
      } as any);

      vi.mocked(prisma.attendanceSession.update).mockResolvedValue({
        id: sessionId,
        status: SessionStatus.ditutup,
      } as any);

      const res = await request(app)
        .patch(`/api/v1/sessions/${sessionId}/close`)
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data.status).toBe('ditutup');
      expect(prisma.auditLog.create).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            action: 'SESSION_CLOSED',
            entity_id: sessionId,
          }),
        })
      );
    });

    it('Guru of another class: should be rejected from closing (403 FORBIDDEN)', async () => {
      vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue({
        id: sessionId,
        status: SessionStatus.terbuka,
        schedule: { teacher_id: guruClassOwnerId },
      } as any);

      const res = await request(app)
        .patch(`/api/v1/sessions/${sessionId}/close`)
        .set('Authorization', `Bearer ${guruOtherToken}`);

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });
  });

  describe('POST /api/v1/sessions/:id/challenge (Challenge Nonce Siswa)', () => {
    it('Siswa: should generate random nonce with 2-minute TTL when session is open and student in class', async () => {
      const now = new Date();
      vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue({
        id: sessionId,
        status: SessionStatus.terbuka,
        open_at: new Date(now.getTime() - 10 * 60 * 1000), // opened 10 min ago
        close_at: new Date(now.getTime() + 50 * 60 * 1000), // closes in 50 min
        schedule: {
          class: {
            members: [{ user_id: siswaId }],
          },
        },
      } as any);

      vi.mocked(prisma.attendanceChallenge.create).mockResolvedValue({} as any);

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/challenge`)
        .set('Authorization', `Bearer ${siswaToken}`);

      expect(res.status).toBe(201);
      expect(res.body.data.nonce).toBeDefined();
      expect(res.body.data.expiresAt).toBeDefined();
      expect(res.body.data.serverTime).toBeDefined();

      // Check TTL ~ 2 minutes
      const expiresAt = new Date(res.body.data.expiresAt).getTime();
      const serverTime = new Date(res.body.data.serverTime).getTime();
      expect(expiresAt - serverTime).toBe(2 * 60 * 1000);
    });

    it('Siswa: should reject challenge with SESSION_CLOSED when session status is not terbuka', async () => {
      vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue({
        id: sessionId,
        status: SessionStatus.terjadwal,
        open_at: new Date(Date.now() + 10000),
        close_at: new Date(Date.now() + 50000),
        schedule: {
          class: {
            members: [{ user_id: siswaId }],
          },
        },
      } as any);

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/challenge`)
        .set('Authorization', `Bearer ${siswaToken}`);

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('SESSION_CLOSED');
    });

    it('Siswa: should reject challenge with NOT_IN_CLASS when student is not in the class', async () => {
      const now = new Date();
      vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue({
        id: sessionId,
        status: SessionStatus.terbuka,
        open_at: new Date(now.getTime() - 10000),
        close_at: new Date(now.getTime() + 50000),
        schedule: {
          class: {
            members: [], // empty for this student
          },
        },
      } as any);

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/challenge`)
        .set('Authorization', `Bearer ${siswaOtherToken}`);

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('NOT_IN_CLASS');
    });
  });
});
