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
      subject: {
        findMany: vi.fn(),
        findUnique: vi.fn(),
        count: vi.fn(),
        create: vi.fn(),
        update: vi.fn(),
        delete: vi.fn(),
      },
      schedule: {
        count: vi.fn(),
      },
    },
  };
});

describe('Subjects Module Tests', () => {
  const operatorToken = signAccessToken({ sub: 'user-op', role: 'operator' });
  const validSubjectId = '44444444-4444-4444-4444-444444444444';

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

  describe('GET /api/v1/subjects', () => {
    it('should return paginated list of subjects', async () => {
      vi.mocked(prisma.subject.count).mockResolvedValue(1);
      vi.mocked(prisma.subject.findMany).mockResolvedValue([
        { id: validSubjectId, nama: 'Matematika', _count: { schedules: 2 } } as any,
      ]);

      const res = await request(app)
        .get('/api/v1/subjects')
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toHaveLength(1);
      expect(res.body.data[0].nama).toBe('Matematika');
    });
  });

  describe('POST /api/v1/subjects', () => {
    it('should create subject successfully', async () => {
      vi.mocked(prisma.subject.create).mockResolvedValue({
        id: validSubjectId,
        nama: 'Bahasa Indonesia',
      } as any);

      const res = await request(app)
        .post('/api/v1/subjects')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({ nama: 'Bahasa Indonesia' });

      expect(res.status).toBe(201);
      expect(res.body.data.nama).toBe('Bahasa Indonesia');
    });

    it('should reject with 400 when nama is empty', async () => {
      const res = await request(app)
        .post('/api/v1/subjects')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({ nama: '' });

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('VALIDATION_ERROR');
    });
  });

  describe('DELETE /api/v1/subjects/:id', () => {
    it('should return 409 IN_USE if subject is referenced in schedules', async () => {
      vi.mocked(prisma.subject.findUnique).mockResolvedValue({
        id: validSubjectId,
        nama: 'Fisika',
      } as any);
      vi.mocked(prisma.schedule.count).mockResolvedValue(3);

      const res = await request(app)
        .delete(`/api/v1/subjects/${validSubjectId}`)
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(409);
      expect(res.body.error.code).toBe('IN_USE');
    });

    it('should delete subject if no schedules reference it', async () => {
      vi.mocked(prisma.subject.findUnique).mockResolvedValue({
        id: validSubjectId,
        nama: 'Fisika',
      } as any);
      vi.mocked(prisma.schedule.count).mockResolvedValue(0);
      vi.mocked(prisma.subject.delete).mockResolvedValue({} as any);

      const res = await request(app)
        .delete(`/api/v1/subjects/${validSubjectId}`)
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(200);
      expect(res.body.message).toContain('berhasil dihapus');
    });
  });
});
