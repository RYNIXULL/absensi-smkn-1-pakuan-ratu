import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from 'supertest';
import argon2 from 'argon2';
import { app } from '../src/app';
import { prisma } from '../src/lib/prisma';
import { hashToken, signAccessToken } from '../src/lib/token';

vi.mock('../src/lib/prisma', () => {
  return {
    prisma: {
      user: {
        findFirst: vi.fn(),
        findUnique: vi.fn(),
        update: vi.fn(),
      },
      device: {
        findMany: vi.fn(),
        upsert: vi.fn(),
      },
      refreshToken: {
        findFirst: vi.fn(),
        create: vi.fn(),
        update: vi.fn(),
        updateMany: vi.fn(),
      },
      auditLog: {
        create: vi.fn().mockResolvedValue({}),
      },
      $transaction: vi.fn((fns) => Promise.all(Array.isArray(fns) ? fns : [fns])),
    },
  };
});

describe('Auth Module Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('POST /api/v1/auth/login', () => {
    it('should successfully log in and return access/refresh tokens for valid credentials', async () => {
      const passwordHash = await argon2.hash('Password123!', { type: argon2.argon2id });
      const mockUser = {
        id: 'user-1',
        nama: 'Ahmad Siswa',
        nomor_induk: 'SISWA001',
        email: 'ahmad@sekolah.sch.id',
        password_hash: passwordHash,
        role: 'siswa',
        is_active: true,
      };

      vi.mocked(prisma.user.findFirst).mockResolvedValue(mockUser as any);
      vi.mocked(prisma.device.findMany).mockResolvedValue([]);
      vi.mocked(prisma.device.upsert).mockResolvedValue({} as any);
      vi.mocked(prisma.refreshToken.create).mockResolvedValue({} as any);

      const res = await request(app)
        .post('/api/v1/auth/login')
        .send({
          identifier: 'SISWA001',
          password: 'Password123!',
          device: {
            device_id: 'device-abc-123',
            model: 'Samsung Galaxy A54',
          },
        });

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty('accessToken');
      expect(res.body).toHaveProperty('refreshToken');
      expect(res.body.user).toEqual({
        id: 'user-1',
        role: 'siswa',
        nama: 'Ahmad Siswa',
      });
      // 1st device for student is automatically active and approved
      expect(prisma.device.upsert).toHaveBeenCalledWith(
        expect.objectContaining({
          create: expect.objectContaining({
            status: 'disetujui',
            is_active: true,
          }),
        }),
      );
    });

    it('should reject with uniform INVALID_CREDENTIALS when user is not found (anti-enumeration)', async () => {
      vi.mocked(prisma.user.findFirst).mockResolvedValue(null);

      const res = await request(app)
        .post('/api/v1/auth/login')
        .send({
          identifier: 'UNKNOWN001',
          password: 'Password123!',
          device: { device_id: 'dev-1' },
        });

      expect(res.status).toBe(401);
      expect(res.body.error).toEqual({
        code: 'INVALID_CREDENTIALS',
        message: 'Nomor induk/email atau password salah',
      });
    });

    it('should reject with uniform INVALID_CREDENTIALS when password does not match', async () => {
      const passwordHash = await argon2.hash('CorrectPassword!', { type: argon2.argon2id });
      const mockUser = {
        id: 'user-1',
        nomor_induk: 'SISWA001',
        password_hash: passwordHash,
        role: 'siswa',
        is_active: true,
      };

      vi.mocked(prisma.user.findFirst).mockResolvedValue(mockUser as any);

      const res = await request(app)
        .post('/api/v1/auth/login')
        .send({
          identifier: 'SISWA001',
          password: 'WrongPassword!',
          device: { device_id: 'dev-1' },
        });

      expect(res.status).toBe(401);
      expect(res.body.error).toEqual({
        code: 'INVALID_CREDENTIALS',
        message: 'Nomor induk/email atau password salah',
      });
    });

    it('should reject with 403 ACCOUNT_INACTIVE when user account is disabled', async () => {
      const passwordHash = await argon2.hash('Password123!', { type: argon2.argon2id });
      const mockUser = {
        id: 'user-inactive',
        nomor_induk: 'SISWA002',
        password_hash: passwordHash,
        role: 'siswa',
        is_active: false,
      };

      vi.mocked(prisma.user.findFirst).mockResolvedValue(mockUser as any);

      const res = await request(app)
        .post('/api/v1/auth/login')
        .send({
          identifier: 'SISWA002',
          password: 'Password123!',
          device: { device_id: 'dev-1' },
        });

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('ACCOUNT_INACTIVE');
    });

    it('should reject student login from a 2nd different device with 403 DEVICE_PENDING and no token', async () => {
      const passwordHash = await argon2.hash('Password123!', { type: argon2.argon2id });
      const mockUser = {
        id: 'user-1',
        nama: 'Ahmad Siswa',
        nomor_induk: 'SISWA001',
        password_hash: passwordHash,
        role: 'siswa',
        is_active: true,
      };

      vi.mocked(prisma.user.findFirst).mockResolvedValue(mockUser as any);
      // Student already has active approved device: 'device-phone-original'
      vi.mocked(prisma.device.findMany).mockResolvedValue([
        {
          id: 'dev-reg-1',
          device_id: 'device-phone-original',
          status: 'disetujui',
          is_active: true,
        } as any,
      ]);
      vi.mocked(prisma.device.upsert).mockResolvedValue({} as any);

      const res = await request(app)
        .post('/api/v1/auth/login')
        .send({
          identifier: 'SISWA001',
          password: 'Password123!',
          device: {
            device_id: 'device-friend-phone-new',
            model: 'Xiaomi Redmi Note 12',
          },
        });

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('DEVICE_PENDING');
      expect(res.body.accessToken).toBeUndefined();
      // Device should be registered as 'menunggu'
      expect(prisma.device.upsert).toHaveBeenCalledWith(
        expect.objectContaining({
          create: expect.objectContaining({
            status: 'menunggu',
            is_active: false,
          }),
        }),
      );
    });
  });

  describe('POST /api/v1/auth/refresh', () => {
    it('should rotate refresh token and return new tokens', async () => {
      const oldRawToken = 'test-refresh-token-valid-1234567890';
      const oldHash = hashToken(oldRawToken);

      const mockTokenRecord = {
        id: 'token-rec-1',
        user_id: 'user-1',
        token_hash: oldHash,
        expires_at: new Date(Date.now() + 3600000), // 1 hour ahead
        revoked_at: null,
      };

      const mockUser = {
        id: 'user-1',
        role: 'siswa',
        is_active: true,
      };

      vi.mocked(prisma.refreshToken.findFirst).mockResolvedValue(mockTokenRecord as any);
      vi.mocked(prisma.user.findUnique).mockResolvedValue(mockUser as any);
      vi.mocked(prisma.refreshToken.update).mockResolvedValue({} as any);
      vi.mocked(prisma.refreshToken.create).mockResolvedValue({} as any);

      const res = await request(app)
        .post('/api/v1/auth/refresh')
        .send({ refreshToken: oldRawToken });

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty('accessToken');
      expect(res.body).toHaveProperty('refreshToken');
      expect(res.body.refreshToken).not.toBe(oldRawToken);
    });

    it('should detect reuse of revoked token and revoke ALL user refresh tokens', async () => {
      const stolenToken = 'stolen-revoked-token-12345';
      const stolenHash = hashToken(stolenToken);

      const mockTokenRecord = {
        id: 'token-stolen-id',
        user_id: 'victim-user-id',
        token_hash: stolenHash,
        expires_at: new Date(Date.now() + 3600000),
        revoked_at: new Date(Date.now() - 60000), // ALREADY REVOKED!
      };

      vi.mocked(prisma.refreshToken.findFirst).mockResolvedValue(mockTokenRecord as any);
      vi.mocked(prisma.refreshToken.updateMany).mockResolvedValue({ count: 5 } as any);

      const res = await request(app)
        .post('/api/v1/auth/refresh')
        .send({ refreshToken: stolenToken });

      expect(res.status).toBe(401);
      expect(res.body.error.code).toBe('INVALID_REFRESH_TOKEN');
      // All active refresh tokens of victim revoked!
      expect(prisma.refreshToken.updateMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: {
            user_id: 'victim-user-id',
            revoked_at: null,
          },
          data: {
            revoked_at: expect.any(Date),
          },
        }),
      );
    });
  });

  describe('POST /api/v1/auth/change-password', () => {
    it('should change password and revoke active refresh tokens when old password is valid', async () => {
      const oldPasswordHash = await argon2.hash('OldPassword123!', { type: argon2.argon2id });
      const mockUser = {
        id: 'user-change-pw',
        nomor_induk: 'OP001',
        password_hash: oldPasswordHash,
        role: 'operator',
        is_active: true,
      };

      const validToken = signAccessToken({ sub: 'user-change-pw', role: 'operator' });

      vi.mocked(prisma.user.findUnique).mockResolvedValue(mockUser as any);
      vi.mocked(prisma.user.update).mockResolvedValue({} as any);
      vi.mocked(prisma.refreshToken.updateMany).mockResolvedValue({ count: 1 } as any);

      const res = await request(app)
        .post('/api/v1/auth/change-password')
        .set('Authorization', `Bearer ${validToken}`)
        .send({
          oldPassword: 'OldPassword123!',
          newPassword: 'BrandNewSecurePassword123!',
        });

      expect(res.status).toBe(200);
      expect(res.body.message).toBe('Password berhasil diubah');
      expect(prisma.user.update).toHaveBeenCalled();
      expect(prisma.refreshToken.updateMany).toHaveBeenCalledWith({
        where: { user_id: 'user-change-pw', revoked_at: null },
        data: { revoked_at: expect.any(Date) },
      });
    });

    it('should reject password change when old password is wrong', async () => {
      const oldPasswordHash = await argon2.hash('OldPassword123!', { type: argon2.argon2id });
      const mockUser = {
        id: 'user-change-pw',
        password_hash: oldPasswordHash,
        role: 'operator',
        is_active: true,
      };

      const validToken = signAccessToken({ sub: 'user-change-pw', role: 'operator' });
      vi.mocked(prisma.user.findUnique).mockResolvedValue(mockUser as any);

      const res = await request(app)
        .post('/api/v1/auth/change-password')
        .set('Authorization', `Bearer ${validToken}`)
        .send({
          oldPassword: 'WrongOldPassword!',
          newPassword: 'BrandNewSecurePassword123!',
        });

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('INVALID_CREDENTIALS');
    });
  });
});
