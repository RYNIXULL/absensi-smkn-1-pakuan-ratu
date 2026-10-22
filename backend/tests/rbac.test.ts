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
        count: vi.fn(),
      },
      device: {
        findMany: vi.fn(),
        findUnique: vi.fn(),
        update: vi.fn(),
        updateMany: vi.fn(),
      },
      schedule: {
        findFirst: vi.fn(),
      },
      auditLog: {
        create: vi.fn().mockResolvedValue({}),
      },
      $transaction: vi.fn((fns) => Promise.all(Array.isArray(fns) ? fns : [fns])),
    },
  };
});

describe('RBAC Matrix Tests', () => {
  const operatorToken = signAccessToken({ sub: 'user-op', role: 'operator' });
  const guruToken = signAccessToken({ sub: 'user-guru', role: 'guru' });
  const siswaToken = signAccessToken({ sub: 'user-siswa', role: 'siswa' });

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const setupMockUser = (id: string, role: 'operator' | 'guru' | 'siswa') => {
    vi.mocked(prisma.user.findUnique).mockResolvedValue({
      id,
      nama: `User ${role}`,
      nomor_induk: `ID_${role}`,
      email: `${role}@sekolah.local`,
      role,
      is_active: true,
      created_at: new Date(),
    } as any);
  };

  describe('GET /api/v1/users (Operator Only)', () => {
    it('Operator: ALLOWED (200)', async () => {
      setupMockUser('user-op', 'operator');
      vi.mocked(prisma.user.count).mockResolvedValue(1);
      vi.mocked(prisma.user.findMany).mockResolvedValue([]);

      const res = await request(app)
        .get('/api/v1/users')
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty('data');
    });

    it('Guru: FORBIDDEN (403)', async () => {
      setupMockUser('user-guru', 'guru');

      const res = await request(app)
        .get('/api/v1/users')
        .set('Authorization', `Bearer ${guruToken}`);

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    it('Siswa: FORBIDDEN (403)', async () => {
      setupMockUser('user-siswa', 'siswa');

      const res = await request(app)
        .get('/api/v1/users')
        .set('Authorization', `Bearer ${siswaToken}`);

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    it('Unauthenticated: UNAUTHORIZED (401)', async () => {
      const res = await request(app).get('/api/v1/users');
      expect(res.status).toBe(401);
      expect(res.body.error.code).toBe('UNAUTHORIZED');
    });
  });

  describe('GET /api/v1/devices/requests (Operator Only)', () => {
    it('Operator: ALLOWED (200)', async () => {
      setupMockUser('user-op', 'operator');
      vi.mocked(prisma.device.findMany).mockResolvedValue([]);

      const res = await request(app)
        .get('/api/v1/devices/requests')
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty('data');
    });

    it('Guru: FORBIDDEN (403)', async () => {
      setupMockUser('user-guru', 'guru');

      const res = await request(app)
        .get('/api/v1/devices/requests')
        .set('Authorization', `Bearer ${guruToken}`);

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    it('Siswa: FORBIDDEN (403)', async () => {
      setupMockUser('user-siswa', 'siswa');

      const res = await request(app)
        .get('/api/v1/devices/requests')
        .set('Authorization', `Bearer ${siswaToken}`);

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });
  });

  describe('GET /api/v1/me (All Roles Allowed)', () => {
    it('Operator: ALLOWED (200)', async () => {
      setupMockUser('user-op', 'operator');
      const res = await request(app)
        .get('/api/v1/me')
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(200);
      expect(res.body.role).toBe('operator');
    });

    it('Guru: ALLOWED (200)', async () => {
      setupMockUser('user-guru', 'guru');
      const res = await request(app)
        .get('/api/v1/me')
        .set('Authorization', `Bearer ${guruToken}`);

      expect(res.status).toBe(200);
      expect(res.body.role).toBe('guru');
    });

    it('Siswa: ALLOWED (200)', async () => {
      setupMockUser('user-siswa', 'siswa');
      const res = await request(app)
        .get('/api/v1/me')
        .set('Authorization', `Bearer ${siswaToken}`);

      expect(res.status).toBe(200);
      expect(res.body.role).toBe('siswa');
    });

    it('Unauthenticated: UNAUTHORIZED (401)', async () => {
      const res = await request(app).get('/api/v1/me');
      expect(res.status).toBe(401);
      expect(res.body.error.code).toBe('UNAUTHORIZED');
    });
  });
});
