import argon2 from 'argon2';
import { RequestStatus, UserRole } from '@prisma/client';
import { prisma } from '../../lib/prisma';
import { AppError } from '../../errors/appError';
import {
  generateRefreshToken,
  hashToken,
  signAccessToken,
} from '../../lib/token';
import { logAudit } from '../../lib/audit';
import { ChangePasswordInput, LoginInput } from './auth.schema';

export class AuthService {
  async login(input: LoginInput) {
    const identifier = input.identifier.trim();

    // Find user by nomor_induk OR email
    const user = await prisma.user.findFirst({
      where: {
        OR: [
          { nomor_induk: identifier },
          { email: identifier },
        ],
      },
    });

    // Anti-enumeration: uniform error message
    if (!user) {
      throw new AppError('INVALID_CREDENTIALS', 'Nomor induk/email atau password salah', 401);
    }

    const passwordMatch = await argon2.verify(user.password_hash, input.password);
    if (!passwordMatch) {
      throw new AppError('INVALID_CREDENTIALS', 'Nomor induk/email atau password salah', 401);
    }

    if (!user.is_active) {
      throw new AppError('ACCOUNT_INACTIVE', 'Akun Anda telah dinonaktifkan', 403);
    }

    // Device Binding Logic
    if (user.role === UserRole.siswa) {
      const existingDevices = await prisma.device.findMany({
        where: { user_id: user.id },
      });

      const activeApprovedDevice = existingDevices.find(
        (d) => d.is_active && d.status === RequestStatus.disetujui,
      );

      if (!activeApprovedDevice) {
        // First device: automatically active and approved
        await prisma.device.upsert({
          where: {
            user_id_device_id: {
              user_id: user.id,
              device_id: input.device.device_id,
            },
          },
          update: {
            status: RequestStatus.disetujui,
            is_active: true,
            model: input.device.model,
          },
          create: {
            user_id: user.id,
            device_id: input.device.device_id,
            model: input.device.model,
            status: RequestStatus.disetujui,
            is_active: true,
          },
        });
      } else if (activeApprovedDevice.device_id !== input.device.device_id) {
        // Different device: register as 'menunggu' if not existing, reject with 403 DEVICE_PENDING
        await prisma.device.upsert({
          where: {
            user_id_device_id: {
              user_id: user.id,
              device_id: input.device.device_id,
            },
          },
          update: {
            model: input.device.model,
          },
          create: {
            user_id: user.id,
            device_id: input.device.device_id,
            model: input.device.model,
            status: RequestStatus.menunggu,
            is_active: false,
          },
        });

        await logAudit({
          actor_id: user.id,
          action: 'DEVICE_CHANGE_REQUESTED',
          entity: 'Device',
          new_value: { device_id: input.device.device_id, model: input.device.model },
        });

        throw new AppError(
          'DEVICE_PENDING',
          'Perangkat baru terdeteksi dan sedang menunggu persetujuan operator',
          403,
        );
      }
    } else {
      // Guru & Operator: multiple devices allowed, auto-approved
      await prisma.device.upsert({
        where: {
          user_id_device_id: {
            user_id: user.id,
            device_id: input.device.device_id,
          },
        },
        update: {
          status: RequestStatus.disetujui,
          is_active: true,
          model: input.device.model,
        },
        create: {
          user_id: user.id,
          device_id: input.device.device_id,
          model: input.device.model,
          status: RequestStatus.disetujui,
          is_active: true,
        },
      });
    }

    // Generate tokens
    const accessToken = signAccessToken({ sub: user.id, role: user.role });
    const rawRefreshToken = generateRefreshToken();
    const tokenHash = hashToken(rawRefreshToken);

    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 7); // 7 days

    await prisma.refreshToken.create({
      data: {
        user_id: user.id,
        token_hash: tokenHash,
        expires_at: expiresAt,
      },
    });

    return {
      accessToken,
      refreshToken: rawRefreshToken,
      user: {
        id: user.id,
        role: user.role,
        nama: user.nama,
      },
    };
  }

  async refresh(rawRefreshToken: string) {
    const tokenHash = hashToken(rawRefreshToken);

    const tokenRecord = await prisma.refreshToken.findFirst({
      where: { token_hash: tokenHash },
    });

    if (!tokenRecord) {
      throw new AppError('INVALID_REFRESH_TOKEN', 'Refresh token tidak valid', 401);
    }

    // Reuse Detection
    if (tokenRecord.revoked_at !== null) {
      // Revoke ALL active tokens of this user!
      await prisma.refreshToken.updateMany({
        where: {
          user_id: tokenRecord.user_id,
          revoked_at: null,
        },
        data: {
          revoked_at: new Date(),
        },
      });

      await logAudit({
        actor_id: tokenRecord.user_id,
        action: 'REFRESH_TOKEN_REUSE_DETECTED',
        entity: 'RefreshToken',
        entity_id: tokenRecord.id,
      });

      throw new AppError(
        'INVALID_REFRESH_TOKEN',
        'Token telah dicabut karena indikasi penyalahgunaan',
        401,
      );
    }

    // Check expiration
    if (tokenRecord.expires_at < new Date()) {
      await prisma.refreshToken.update({
        where: { id: tokenRecord.id },
        data: { revoked_at: new Date() },
      });
      throw new AppError('REFRESH_TOKEN_EXPIRED', 'Refresh token telah kedaluwarsa', 401);
    }

    const user = await prisma.user.findUnique({
      where: { id: tokenRecord.user_id },
    });

    if (!user || !user.is_active) {
      throw new AppError('ACCOUNT_INACTIVE', 'Akun pengguna tidak aktif', 403);
    }

    // Rotate token in transaction
    const newRawRefreshToken = generateRefreshToken();
    const newTokenHash = hashToken(newRawRefreshToken);

    const newExpiresAt = new Date();
    newExpiresAt.setDate(newExpiresAt.getDate() + 7);

    await prisma.$transaction([
      prisma.refreshToken.update({
        where: { id: tokenRecord.id },
        data: { revoked_at: new Date() },
      }),
      prisma.refreshToken.create({
        data: {
          user_id: user.id,
          token_hash: newTokenHash,
          expires_at: newExpiresAt,
        },
      }),
    ]);

    const newAccessToken = signAccessToken({ sub: user.id, role: user.role });

    return {
      accessToken: newAccessToken,
      refreshToken: newRawRefreshToken,
    };
  }

  async logout(userId: string, rawRefreshToken?: string) {
    if (rawRefreshToken) {
      const tokenHash = hashToken(rawRefreshToken);
      await prisma.refreshToken.updateMany({
        where: {
          user_id: userId,
          token_hash: tokenHash,
          revoked_at: null,
        },
        data: {
          revoked_at: new Date(),
        },
      });
    } else {
      // Revoke all active refresh tokens
      await prisma.refreshToken.updateMany({
        where: {
          user_id: userId,
          revoked_at: null,
        },
        data: {
          revoked_at: new Date(),
        },
      });
    }

    return { message: 'Berhasil logout' };
  }

  async changePassword(userId: string, input: ChangePasswordInput) {
    const user = await prisma.user.findUnique({
      where: { id: userId },
    });

    if (!user) {
      throw new AppError('USER_NOT_FOUND', 'Pengguna tidak ditemukan', 404);
    }

    const match = await argon2.verify(user.password_hash, input.oldPassword);
    if (!match) {
      throw new AppError('INVALID_CREDENTIALS', 'Password lama salah', 400);
    }

    const newPasswordHash = await argon2.hash(input.newPassword, {
      type: argon2.argon2id,
    });

    await prisma.$transaction([
      prisma.user.update({
        where: { id: userId },
        data: { password_hash: newPasswordHash },
      }),
      prisma.refreshToken.updateMany({
        where: {
          user_id: userId,
          revoked_at: null,
        },
        data: {
          revoked_at: new Date(),
        },
      }),
    ]);

    await logAudit({
      actor_id: userId,
      action: 'PASSWORD_CHANGED',
      entity: 'User',
      entity_id: userId,
    });

    return { message: 'Password berhasil diubah' };
  }
}

export const authService = new AuthService();
