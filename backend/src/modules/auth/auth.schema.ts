import { z } from 'zod';

export const loginSchema = z.object({
  identifier: z.string().min(1, 'Nomor induk atau email wajib diisi'),
  password: z.string().min(1, 'Password wajib diisi'),
  device: z.object({
    device_id: z.string().min(1, 'Device ID wajib diisi'),
    model: z.string().optional(),
  }),
});

export const refreshTokenSchema = z.object({
  refreshToken: z.string().min(1, 'Refresh token wajib diisi'),
});

export const changePasswordSchema = z.object({
  oldPassword: z.string().min(1, 'Password lama wajib diisi'),
  newPassword: z.string().min(8, 'Password baru minimal 8 karakter'),
});

export type LoginInput = z.infer<typeof loginSchema>;
export type RefreshTokenInput = z.infer<typeof refreshTokenSchema>;
export type ChangePasswordInput = z.infer<typeof changePasswordSchema>;
