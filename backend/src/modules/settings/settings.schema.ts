import { z } from 'zod';

export const updateSettingSchema = z.object({
  key: z.string().min(1, 'Key tidak boleh kosong'),
  value: z.string(),
});

export type UpdateSettingInput = z.infer<typeof updateSettingSchema>;
