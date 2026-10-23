import { z } from 'zod';

export const createClassSchema = z.object({
  nama: z.string().trim().min(1, 'Nama kelas wajib diisi'),
  tahun_ajaran: z.string().trim().min(1, 'Tahun ajaran wajib diisi'),
});

export const updateClassSchema = z.object({
  nama: z.string().trim().min(1, 'Nama kelas tidak boleh kosong').optional(),
  tahun_ajaran: z.string().trim().min(1, 'Tahun ajaran tidak boleh kosong').optional(),
});

export const listClassQuerySchema = z.object({
  page: z.coerce.number().int().positive().default(1),
  pageSize: z.coerce.number().int().positive().max(100).default(20),
  q: z.string().optional(),
  tahun_ajaran: z.string().optional(),
});

export const addClassMembersSchema = z.object({
  user_ids: z.array(z.string().uuid('ID pengguna harus berupa UUID valid')).min(1, 'Minimal satu user_id harus disertakan'),
});

export type CreateClassInput = z.infer<typeof createClassSchema>;
export type UpdateClassInput = z.infer<typeof updateClassSchema>;
export type ListClassQuery = z.infer<typeof listClassQuerySchema>;
export type AddClassMembersInput = z.infer<typeof addClassMembersSchema>;
