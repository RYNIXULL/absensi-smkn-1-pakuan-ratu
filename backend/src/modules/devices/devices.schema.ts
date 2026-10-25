import { z } from 'zod';

export const updateDeviceRequestSchema = z.object({
  action: z.enum(['setujui', 'tolak'], {
    errorMap: () => ({ message: "Aksi harus 'setujui' atau 'tolak'" }),
  }),
});

export type UpdateDeviceRequestInput = z.infer<typeof updateDeviceRequestSchema>;

export const updateFcmTokenSchema = z.object({
  device_id: z.string().min(1, 'device_id wajib diisi'),
  fcm_token: z.string().min(1, 'fcm_token wajib diisi'),
});

export type UpdateFcmTokenInput = z.infer<typeof updateFcmTokenSchema>;
