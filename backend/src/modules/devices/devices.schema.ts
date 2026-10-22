import { z } from 'zod';

export const updateDeviceRequestSchema = z.object({
  action: z.enum(['setujui', 'tolak'], {
    errorMap: () => ({ message: "Aksi harus 'setujui' atau 'tolak'" }),
  }),
});

export type UpdateDeviceRequestInput = z.infer<typeof updateDeviceRequestSchema>;
