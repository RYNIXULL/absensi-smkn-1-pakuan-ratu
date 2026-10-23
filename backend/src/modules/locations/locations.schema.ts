import { z } from 'zod';

export const createLocationSchema = z.object({
  nama: z.string().trim().min(1, 'Nama lokasi wajib diisi'),
  latitude: z.coerce
    .number()
    .min(-90, 'Latitude harus di antara -90 dan 90')
    .max(90, 'Latitude harus di antara -90 dan 90'),
  longitude: z.coerce
    .number()
    .min(-180, 'Longitude harus di antara -180 dan 180')
    .max(180, 'Longitude harus di antara -180 dan 180'),
  radius_m: z.coerce
    .number()
    .int('Radius harus berupa bilangan bulat')
    .min(10, 'Radius minimal 10 meter')
    .max(1000, 'Radius maksimal 1000 meter'),
  is_active: z.boolean().default(true),
});

export const updateLocationSchema = z.object({
  nama: z.string().trim().min(1, 'Nama lokasi tidak boleh kosong').optional(),
  latitude: z.coerce
    .number()
    .min(-90, 'Latitude harus di antara -90 dan 90')
    .max(90, 'Latitude harus di antara -90 dan 90')
    .optional(),
  longitude: z.coerce
    .number()
    .min(-180, 'Longitude harus di antara -180 dan 180')
    .max(180, 'Longitude harus di antara -180 dan 180')
    .optional(),
  radius_m: z.coerce
    .number()
    .int('Radius harus berupa bilangan bulat')
    .min(10, 'Radius minimal 10 meter')
    .max(1000, 'Radius maksimal 1000 meter')
    .optional(),
  is_active: z.boolean().optional(),
});

export const listLocationQuerySchema = z.object({
  page: z.coerce.number().int().positive().default(1),
  pageSize: z.coerce.number().int().positive().max(100).default(20),
  q: z.string().optional(),
  is_active: z
    .enum(['true', 'false'])
    .transform((val) => val === 'true')
    .optional(),
});

export type CreateLocationInput = z.infer<typeof createLocationSchema>;
export type UpdateLocationInput = z.infer<typeof updateLocationSchema>;
export type ListLocationQuery = z.infer<typeof listLocationQuerySchema>;
