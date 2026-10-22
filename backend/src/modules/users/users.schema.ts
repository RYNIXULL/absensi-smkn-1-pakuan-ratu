import { z } from 'zod';

export const createUserSchema = z.object({
  nama: z.string().min(1, 'Nama wajib diisi'),
  nomor_induk: z.string().min(1, 'Nomor induk wajib diisi'),
  email: z.string().email('Format email tidak valid').optional(),
  role: z.enum(['operator', 'guru', 'siswa'], {
    errorMap: () => ({ message: "Peran harus salah satu dari 'operator', 'guru', atau 'siswa'" }),
  }),
  password: z.string().min(8, 'Password minimal 8 karakter').optional(),
});

export const updateUserSchema = z.object({
  nama: z.string().min(1, 'Nama tidak boleh kosong').optional(),
  email: z.string().email('Format email tidak valid').optional(),
  role: z.enum(['operator', 'guru', 'siswa']).optional(),
  is_active: z.boolean().optional(),
});

export const userQuerySchema = z.object({
  page: z.coerce.number().int().positive().default(1),
  pageSize: z.coerce.number().int().positive().max(100).default(20),
  q: z.string().optional(),
  role: z.enum(['operator', 'guru', 'siswa']).optional(),
  is_active: z
    .preprocess((val) => (val === 'true' ? true : val === 'false' ? false : val), z.boolean())
    .optional(),
});

export type CreateUserInput = z.infer<typeof createUserSchema>;
export type UpdateUserInput = z.infer<typeof updateUserSchema>;
export type UserQueryInput = z.infer<typeof userQuerySchema>;
