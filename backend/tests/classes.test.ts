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
        findMany: vi.fn(),
      },
      class: {
        findMany: vi.fn(),
        findUnique: vi.fn(),
        count: vi.fn(),
        create: vi.fn(),
        update: vi.fn(),
        delete: vi.fn(),
      },
      classMember: {
        findMany: vi.fn(),
        findUnique: vi.fn(),
        count: vi.fn(),
        create: vi.fn(),
        delete: vi.fn(),
      },
      schedule: {
        count: vi.fn(),
      },
      $transaction: vi.fn((fns) => Promise.all(Array.isArray(fns) ? fns : [fns])),
    },
  };
});

describe('Classes Module Tests', () => {
  const operatorToken = signAccessToken({ sub: 'user-op', role: 'operator' });
  const validClassId = '11111111-1111-1111-1111-111111111111';
  const validUserId1 = '22222222-2222-2222-2222-222222222222';
  const validUserId2 = '33333333-3333-3333-3333-333333333333';

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(prisma.user.findUnique).mockResolvedValue({
      id: 'user-op',
      nama: 'Operator Admin',
      nomor_induk: 'OP001',
      role: 'operator',
      is_active: true,
    } as any);
  });

  describe('GET /api/v1/classes', () => {
    it('should return paginated list of classes', async () => {
      vi.mocked(prisma.class.count).mockResolvedValue(1);
      vi.mocked(prisma.class.findMany).mockResolvedValue([
        {
          id: validClassId,
          nama: 'X RPL 1',
          tahun_ajaran: '2024/2025',
          _count: { members: 30, schedules: 5 },
        } as any,
      ]);

      const res = await request(app)
        .get('/api/v1/classes')
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toHaveLength(1);
      expect(res.body.pagination.total).toBe(1);
      expect(res.body.data[0].nama).toBe('X RPL 1');
    });
  });

  describe('POST /api/v1/classes', () => {
    it('should successfully create a class with valid input', async () => {
      vi.mocked(prisma.class.create).mockResolvedValue({
        id: validClassId,
        nama: 'X RPL 2',
        tahun_ajaran: '2024/2025',
      } as any);

      const res = await request(app)
        .post('/api/v1/classes')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({ nama: 'X RPL 2', tahun_ajaran: '2024/2025' });

      expect(res.status).toBe(201);
      expect(res.body.data.nama).toBe('X RPL 2');
    });

    it('should fail with 400 when missing required fields', async () => {
      const res = await request(app)
        .post('/api/v1/classes')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({ nama: '' });

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('VALIDATION_ERROR');
    });
  });

  describe('DELETE /api/v1/classes/:id', () => {
    it('should prevent deletion with IN_USE when class has schedules', async () => {
      vi.mocked(prisma.class.findUnique).mockResolvedValue({
        id: validClassId,
        nama: 'X RPL 1',
        tahun_ajaran: '2024/2025',
      } as any);
      vi.mocked(prisma.schedule.count).mockResolvedValue(2);

      const res = await request(app)
        .delete(`/api/v1/classes/${validClassId}`)
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(409);
      expect(res.body.error.code).toBe('IN_USE');
    });

    it('should prevent deletion with IN_USE when class has members', async () => {
      vi.mocked(prisma.class.findUnique).mockResolvedValue({
        id: validClassId,
        nama: 'X RPL 1',
        tahun_ajaran: '2024/2025',
      } as any);
      vi.mocked(prisma.schedule.count).mockResolvedValue(0);
      vi.mocked(prisma.classMember.count).mockResolvedValue(5);

      const res = await request(app)
        .delete(`/api/v1/classes/${validClassId}`)
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(409);
      expect(res.body.error.code).toBe('IN_USE');
    });

    it('should delete class successfully when no references exist', async () => {
      vi.mocked(prisma.class.findUnique).mockResolvedValue({
        id: validClassId,
        nama: 'X RPL 1',
        tahun_ajaran: '2024/2025',
      } as any);
      vi.mocked(prisma.schedule.count).mockResolvedValue(0);
      vi.mocked(prisma.classMember.count).mockResolvedValue(0);
      vi.mocked(prisma.class.delete).mockResolvedValue({} as any);

      const res = await request(app)
        .delete(`/api/v1/classes/${validClassId}`)
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(200);
      expect(res.body.message).toContain('berhasil dihapus');
    });
  });

  describe('POST /api/v1/classes/:id/members', () => {
    it('should add multiple members to class successfully', async () => {
      vi.mocked(prisma.class.findUnique).mockResolvedValue({
        id: validClassId,
        nama: 'X RPL 1',
      } as any);
      vi.mocked(prisma.user.findMany).mockResolvedValue([
        { id: validUserId1, nama: 'Siswa 1', role: 'siswa', is_active: true },
        { id: validUserId2, nama: 'Guru 1', role: 'guru', is_active: true },
      ] as any);
      vi.mocked(prisma.classMember.findMany).mockResolvedValue([]);
      vi.mocked(prisma.classMember.create).mockResolvedValue({} as any);

      const res = await request(app)
        .post(`/api/v1/classes/${validClassId}/members`)
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({ user_ids: [validUserId1, validUserId2] });

      expect(res.status).toBe(201);
      expect(res.body.data.count).toBe(2);
    });

    it('should reject with DUPLICATE_MEMBER if user already in class', async () => {
      vi.mocked(prisma.class.findUnique).mockResolvedValue({
        id: validClassId,
        nama: 'X RPL 1',
      } as any);
      vi.mocked(prisma.user.findMany).mockResolvedValue([
        { id: validUserId1, nama: 'Siswa 1', role: 'siswa', is_active: true },
      ] as any);
      vi.mocked(prisma.classMember.findMany).mockResolvedValue([
        { id: 'cm-1', class_id: validClassId, user_id: validUserId1 } as any,
      ]);

      const res = await request(app)
        .post(`/api/v1/classes/${validClassId}/members`)
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({ user_ids: [validUserId1] });

      expect(res.status).toBe(409);
      expect(res.body.error.code).toBe('DUPLICATE_MEMBER');
    });

    it('should reject with INVALID_MEMBER_ROLE if user is operator', async () => {
      vi.mocked(prisma.class.findUnique).mockResolvedValue({
        id: validClassId,
        nama: 'X RPL 1',
      } as any);
      vi.mocked(prisma.user.findMany).mockResolvedValue([
        { id: validUserId1, nama: 'Admin Operator', role: 'operator', is_active: true },
      ] as any);

      const res = await request(app)
        .post(`/api/v1/classes/${validClassId}/members`)
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({ user_ids: [validUserId1] });

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('INVALID_MEMBER_ROLE');
    });
  });

  describe('DELETE /api/v1/classes/:id/members/:userId', () => {
    it('should successfully remove member from class', async () => {
      vi.mocked(prisma.class.findUnique).mockResolvedValue({ id: validClassId } as any);
      vi.mocked(prisma.classMember.findUnique).mockResolvedValue({
        id: 'cm-1',
        class_id: validClassId,
        user_id: validUserId1,
      } as any);
      vi.mocked(prisma.classMember.delete).mockResolvedValue({} as any);

      const res = await request(app)
        .delete(`/api/v1/classes/${validClassId}/members/${validUserId1}`)
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(200);
      expect(res.body.message).toContain('berhasil');
    });
  });
});
