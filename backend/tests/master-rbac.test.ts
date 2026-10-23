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
    },
  };
});

describe('Master Data RBAC Tests', () => {
  const guruToken = signAccessToken({ sub: 'user-guru', role: 'guru' });
  const siswaToken = signAccessToken({ sub: 'user-siswa', role: 'siswa' });

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(prisma.user.findUnique).mockImplementation(async ({ where }: any) => {
      if (where.id === 'user-guru') {
        return { id: 'user-guru', nama: 'Guru', role: 'guru', is_active: true } as any;
      }
      if (where.id === 'user-siswa') {
        return { id: 'user-siswa', nama: 'Siswa', role: 'siswa', is_active: true } as any;
      }
      return null;
    });
  });

  const endpoints = [
    { method: 'get', path: '/api/v1/classes' },
    { method: 'post', path: '/api/v1/classes' },
    { method: 'get', path: '/api/v1/subjects' },
    { method: 'post', path: '/api/v1/subjects' },
    { method: 'get', path: '/api/v1/locations' },
    { method: 'post', path: '/api/v1/locations' },
    { method: 'get', path: '/api/v1/settings' },
    { method: 'patch', path: '/api/v1/settings' },
  ];

  endpoints.forEach(({ method, path }) => {
    it(`Siswa cannot access ${method.toUpperCase()} ${path} (403)`, async () => {
      const req = (request(app) as any)[method](path).set('Authorization', `Bearer ${siswaToken}`);
      const res = await req;
      expect(res.status).toBe(403);
    });

    it(`Guru cannot access ${method.toUpperCase()} ${path} (403)`, async () => {
      const req = (request(app) as any)[method](path).set('Authorization', `Bearer ${guruToken}`);
      const res = await req;
      expect(res.status).toBe(403);
    });
  });
});
