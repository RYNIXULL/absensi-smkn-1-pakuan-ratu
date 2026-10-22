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
        findFirst: vi.fn(),
        create: vi.fn(),
        update: vi.fn(),
      },
      refreshToken: {
        updateMany: vi.fn(),
      },
      auditLog: {
        create: vi.fn().mockResolvedValue({}),
      },
      $transaction: vi.fn((fns) => Promise.all(Array.isArray(fns) ? fns : [fns])),
    },
  };
});

describe('Users Module Tests (Operator CRUD & Reset Password)', () => {
  const operatorToken = signAccessToken({ sub: 'user-op', role: 'operator' });

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(prisma.user.findUnique).mockResolvedValue({
      id: 'user-op',
      nama: 'Super Operator',
      role: 'operator',
      is_active: true,
    } as any);
  });

  it('should create a new user and return initial credentials', async () => {
    vi.mocked(prisma.user.findFirst).mockResolvedValue(null);
    vi.mocked(prisma.user.create).mockResolvedValue({
      id: 'new-user-1',
      nama: 'Guru Matematika',
      nomor_induk: 'GURU001',
      email: 'guru@sekolah.sch.id',
      role: 'guru',
      is_active: true,
      created_at: new Date(),
    } as any);

    const res = await request(app)
      .post('/api/v1/users')
      .set('Authorization', `Bearer ${operatorToken}`)
      .send({
        nama: 'Guru Matematika',
        nomor_induk: 'GURU001',
        email: 'guru@sekolah.sch.id',
        role: 'guru',
      });

    expect(res.status).toBe(201);
    expect(res.body.user.nama).toBe('Guru Matematika');
    expect(res.body).toHaveProperty('initialPassword');
    expect(prisma.user.create).toHaveBeenCalled();
  });

  it('should reject creation when nomor_induk already exists', async () => {
    vi.mocked(prisma.user.findFirst).mockResolvedValue({
      id: 'existing-id',
      nomor_induk: 'GURU001',
    } as any);

    const res = await request(app)
      .post('/api/v1/users')
      .set('Authorization', `Bearer ${operatorToken}`)
      .send({
        nama: 'Guru Lain',
        nomor_induk: 'GURU001',
        role: 'guru',
      });

    expect(res.status).toBe(409);
    expect(res.body.error.code).toBe('CONFLICT');
  });

  it('should update user fields by operator', async () => {
    vi.mocked(prisma.user.findUnique).mockImplementation((args: any) => {
      if (args.where.id === 'user-op') {
        return Promise.resolve({ id: 'user-op', role: 'operator', is_active: true } as any);
      }
      return Promise.resolve({
        id: 'target-user',
        nama: 'Old Name',
        email: 'old@sekolah.sch.id',
        role: 'guru',
        is_active: true,
      } as any);
    });

    vi.mocked(prisma.user.update).mockResolvedValue({
      id: 'target-user',
      nama: 'Updated Name',
      role: 'guru',
      is_active: false,
    } as any);

    const res = await request(app)
      .patch('/api/v1/users/target-user')
      .set('Authorization', `Bearer ${operatorToken}`)
      .send({
        nama: 'Updated Name',
        is_active: false,
      });

    expect(res.status).toBe(200);
    expect(res.body.nama).toBe('Updated Name');
    expect(res.body.is_active).toBe(false);
  });

  it('should reset user password, revoke active refresh tokens, and return temporary password once', async () => {
    vi.mocked(prisma.user.findUnique).mockImplementation((args: any) => {
      if (args.where.id === 'user-op') {
        return Promise.resolve({ id: 'user-op', role: 'operator', is_active: true } as any);
      }
      return Promise.resolve({
        id: 'target-user',
        nama: 'Target User',
        role: 'siswa',
        is_active: true,
      } as any);
    });

    vi.mocked(prisma.user.update).mockResolvedValue({} as any);
    vi.mocked(prisma.refreshToken.updateMany).mockResolvedValue({ count: 2 } as any);

    const res = await request(app)
      .post('/api/v1/users/target-user/reset-password')
      .set('Authorization', `Bearer ${operatorToken}`);

    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('temporaryPassword');
    expect(res.body.temporaryPassword.length).toBeGreaterThanOrEqual(8);
    expect(prisma.refreshToken.updateMany).toHaveBeenCalledWith({
      where: { user_id: 'target-user', revoked_at: null },
      data: { revoked_at: expect.any(Date) },
    });
  });
});
