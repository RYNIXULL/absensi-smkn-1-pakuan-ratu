import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from 'supertest';
import { app } from '../src/app';
import { prisma } from '../src/lib/prisma';
import { signAccessToken } from '../src/lib/token';

vi.mock('../src/lib/prisma', () => {
  return {
    prisma: {
      user: {
        findUnique: vi.fn(),
      },
      class: {
        findUnique: vi.fn(),
      },
      subject: {
        findUnique: vi.fn(),
      },
      location: {
        findUnique: vi.fn(),
      },
      schedule: {
        findMany: vi.fn(),
        findUnique: vi.fn(),
        count: vi.fn(),
        create: vi.fn(),
        update: vi.fn(),
        delete: vi.fn(),
      },
      classMember: {
        findMany: vi.fn(),
      },
      attendanceSession: {
        count: vi.fn(),
      },
      setting: {
        findUnique: vi.fn(),
        findMany: vi.fn(),
        upsert: vi.fn(),
      },
    },
  };
});

describe('Schedules Module Tests', () => {
  const validOperatorId = '00000000-0000-0000-0000-000000000001';
  const validGuruId = '77777777-7777-7777-7777-777777777777';
  const validSiswaId = '00000000-0000-0000-0000-000000000003';
  const fakeSiswaTeacherId = '00000000-0000-0000-0000-000000000004';

  const operatorToken = signAccessToken({ sub: validOperatorId, role: 'operator' });
  const guruToken = signAccessToken({ sub: validGuruId, role: 'guru' });
  const siswaToken = signAccessToken({ sub: validSiswaId, role: 'siswa' });

  const validScheduleId = '66666666-6666-6666-6666-666666666666';
  const validClassId = '11111111-1111-1111-1111-111111111111';
  const validSubjectId = '44444444-4444-4444-4444-444444444444';
  const validLocationId = '55555555-5555-5555-5555-555555555555';

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(prisma.user.findUnique).mockImplementation(async ({ where }: any) => {
      if (where.id === validOperatorId) {
        return { id: validOperatorId, nama: 'Operator', role: 'operator', is_active: true } as any;
      }
      if (where.id === validGuruId) {
        return { id: validGuruId, nama: 'Guru Matematika', role: 'guru', is_active: true } as any;
      }
      if (where.id === validSiswaId) {
        return { id: validSiswaId, nama: 'Siswa', role: 'siswa', is_active: true } as any;
      }
      if (where.id === fakeSiswaTeacherId) {
        return { id: fakeSiswaTeacherId, nama: 'Bukan Guru', role: 'siswa', is_active: true } as any;
      }
      return null;
    });

    vi.mocked(prisma.class.findUnique).mockResolvedValue({ id: validClassId, nama: 'X RPL 1' } as any);
    vi.mocked(prisma.subject.findUnique).mockResolvedValue({ id: validSubjectId, nama: 'Matematika' } as any);
    vi.mocked(prisma.location.findUnique).mockResolvedValue({ id: validLocationId, nama: 'R. 101', is_active: true } as any);
  });

  describe('POST /api/v1/schedules (Validation & Conflict)', () => {
    it('should create schedule successfully when valid and no conflict', async () => {
      vi.mocked(prisma.schedule.findMany).mockResolvedValue([]);
      vi.mocked(prisma.schedule.create).mockResolvedValue({
        id: validScheduleId,
        class_id: validClassId,
        subject_id: validSubjectId,
        teacher_id: validGuruId,
        location_id: validLocationId,
        day_of_week: 1,
        start_time: new Date('1970-01-01T07:30:00.000Z'),
        end_time: new Date('1970-01-01T09:00:00.000Z'),
      } as any);

      vi.mocked(prisma.schedule.findUnique).mockResolvedValue({
        id: validScheduleId,
        day_of_week: 1,
        start_time: new Date('1970-01-01T07:30:00.000Z'),
        end_time: new Date('1970-01-01T09:00:00.000Z'),
        class: { id: validClassId, nama: 'X RPL 1', tahun_ajaran: '2024/2025' },
        subject: { id: validSubjectId, nama: 'Matematika' },
        teacher: { id: validGuruId, nama: 'Guru Matematika', nomor_induk: 'GURU01', email: 'g@s.id' },
        location: { id: validLocationId, nama: 'R. 101', latitude: 0, longitude: 0, radius_m: 50, is_active: true },
      } as any);

      const res = await request(app)
        .post('/api/v1/schedules')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({
          class_id: validClassId,
          subject_id: validSubjectId,
          teacher_id: validGuruId,
          location_id: validLocationId,
          day_of_week: 1,
          start_time: '07:30',
          end_time: '09:00',
        });

      expect(res.status).toBe(201);
      expect(res.body.data.start_time).toBe('07:30');
      expect(res.body.data.end_time).toBe('09:00');
    });

    it('should reject with 400 when start_time >= end_time', async () => {
      const res = await request(app)
        .post('/api/v1/schedules')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({
          class_id: validClassId,
          subject_id: validSubjectId,
          teacher_id: validGuruId,
          location_id: validLocationId,
          day_of_week: 1,
          start_time: '10:00',
          end_time: '09:00',
        });

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('VALIDATION_ERROR');
    });

    it('should reject with 400 INVALID_TEACHER_ROLE when selected user is not guru', async () => {
      const res = await request(app)
        .post('/api/v1/schedules')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({
          class_id: validClassId,
          subject_id: validSubjectId,
          teacher_id: fakeSiswaTeacherId,
          location_id: validLocationId,
          day_of_week: 1,
          start_time: '07:30',
          end_time: '09:00',
        });

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('INVALID_TEACHER_ROLE');
    });

    it('should reject with 409 SCHEDULE_CONFLICT when overlapping schedule exists on the same day', async () => {
      vi.mocked(prisma.schedule.findMany).mockResolvedValue([
        {
          id: 'existing-sched-1',
          teacher_id: validGuruId,
          day_of_week: 1,
          start_time: new Date('1970-01-01T08:00:00.000Z'),
          end_time: new Date('1970-01-01T09:30:00.000Z'),
        } as any,
      ]);

      const res = await request(app)
        .post('/api/v1/schedules')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({
          class_id: validClassId,
          subject_id: validSubjectId,
          teacher_id: validGuruId,
          location_id: validLocationId,
          day_of_week: 1,
          start_time: '07:30',
          end_time: '08:30',
        });

      expect(res.status).toBe(409);
      expect(res.body.error.code).toBe('SCHEDULE_CONFLICT');
    });
  });

  describe('GET /api/v1/me/schedules', () => {
    it('Guru: should return schedules taught by this guru', async () => {
      vi.mocked(prisma.schedule.findMany).mockResolvedValue([
        {
          id: validScheduleId,
          day_of_week: 1,
          start_time: new Date('1970-01-01T07:30:00.000Z'),
          end_time: new Date('1970-01-01T09:00:00.000Z'),
          class: { id: validClassId, nama: 'X RPL 1', tahun_ajaran: '2024/2025' },
          subject: { id: validSubjectId, nama: 'Matematika' },
          teacher: { id: validGuruId, nama: 'Guru Matematika', nomor_induk: 'GURU01' },
          location: { id: validLocationId, nama: 'R. 101', latitude: 0, longitude: 0, radius_m: 50 },
        } as any,
      ]);

      const res = await request(app)
        .get('/api/v1/me/schedules')
        .set('Authorization', `Bearer ${guruToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toHaveLength(1);
      expect(prisma.schedule.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { teacher_id: validGuruId },
        })
      );
    });

    it('Siswa: should return schedules for classes where student is a member', async () => {
      vi.mocked(prisma.classMember.findMany).mockResolvedValue([
        { class_id: validClassId } as any,
      ]);
      vi.mocked(prisma.schedule.findMany).mockResolvedValue([
        {
          id: validScheduleId,
          day_of_week: 1,
          start_time: new Date('1970-01-01T07:30:00.000Z'),
          end_time: new Date('1970-01-01T09:00:00.000Z'),
          class: { id: validClassId, nama: 'X RPL 1', tahun_ajaran: '2024/2025' },
          subject: { id: validSubjectId, nama: 'Matematika' },
          teacher: { id: validGuruId, nama: 'Guru', nomor_induk: 'GURU01' },
          location: { id: validLocationId, nama: 'R. 101', latitude: 0, longitude: 0, radius_m: 50 },
        } as any,
      ]);

      const res = await request(app)
        .get('/api/v1/me/schedules')
        .set('Authorization', `Bearer ${siswaToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toHaveLength(1);
      expect(prisma.schedule.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { class_id: { in: [validClassId] } },
        })
      );
    });
  });

  describe('FR-JDW-03 (Teacher modify schedule permission)', () => {
    it('Guru should be rejected with 403 when teacher_can_edit_schedule setting is false (default)', async () => {
      vi.mocked(prisma.setting.findUnique).mockResolvedValue(null);

      const res = await request(app)
        .post('/api/v1/schedules')
        .set('Authorization', `Bearer ${guruToken}`)
        .send({
          class_id: validClassId,
          subject_id: validSubjectId,
          teacher_id: validGuruId,
          location_id: validLocationId,
          day_of_week: 1,
          start_time: '07:30',
          end_time: '09:00',
        });

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    it('Guru should be allowed when teacher_can_edit_schedule setting is true', async () => {
      vi.mocked(prisma.setting.findUnique).mockResolvedValue({
        key: 'teacher_can_edit_schedule',
        value: 'true',
        updated_at: new Date(),
      } as any);

      vi.mocked(prisma.schedule.findMany).mockResolvedValue([]);
      vi.mocked(prisma.schedule.create).mockResolvedValue({
        id: validScheduleId,
        class_id: validClassId,
        subject_id: validSubjectId,
        teacher_id: validGuruId,
        location_id: validLocationId,
        day_of_week: 1,
        start_time: new Date('1970-01-01T07:30:00.000Z'),
        end_time: new Date('1970-01-01T09:00:00.000Z'),
      } as any);
      vi.mocked(prisma.schedule.findUnique).mockResolvedValue({
        id: validScheduleId,
        day_of_week: 1,
        start_time: new Date('1970-01-01T07:30:00.000Z'),
        end_time: new Date('1970-01-01T09:00:00.000Z'),
        class: { id: validClassId, nama: 'X RPL 1' },
        subject: { id: validSubjectId, nama: 'Matematika' },
        teacher: { id: validGuruId, nama: 'Guru' },
        location: { id: validLocationId, nama: 'R. 101' },
      } as any);

      const res = await request(app)
        .post('/api/v1/schedules')
        .set('Authorization', `Bearer ${guruToken}`)
        .send({
          class_id: validClassId,
          subject_id: validSubjectId,
          teacher_id: validGuruId,
          location_id: validLocationId,
          day_of_week: 1,
          start_time: '07:30',
          end_time: '09:00',
        });

      expect(res.status).toBe(201);
    });
  });

  describe('DELETE /api/v1/schedules/:id', () => {
    it('should reject with 409 IN_USE when schedule has attendance sessions', async () => {
      vi.mocked(prisma.schedule.findUnique).mockResolvedValue({ id: validScheduleId } as any);
      vi.mocked(prisma.attendanceSession.count).mockResolvedValue(2);

      const res = await request(app)
        .delete(`/api/v1/schedules/${validScheduleId}`)
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(409);
      expect(res.body.error.code).toBe('IN_USE');
    });
  });
});
