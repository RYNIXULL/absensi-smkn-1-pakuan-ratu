import { z } from 'zod';

export const createSubjectSchema = z.object({
  nama: z.string().trim().min(1, 'Nama mata pelajaran wajib diisi'),
});

export const updateSubjectSchema = z.object({
  nama: z.string().trim().min(1, 'Nama mata pelajaran tidak boleh kosong'),
});

export const listSubjectQuerySchema = z.object({
  page: z.coerce.number().int().positive().default(1),
  pageSize: z.coerce.number().int().positive().max(100).default(20),
  q: z.string().optional(),
});

export type CreateSubjectInput = z.infer<typeof createSubjectSchema>;
export type UpdateSubjectInput = z.infer<typeof updateSubjectSchema>;
export type ListSubjectQuery = z.infer<typeof listSubjectQuerySchema>;
