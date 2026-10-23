import { z } from 'zod';
import { SessionStatus } from '@prisma/client';

export const createManualSessionSchema = z.object({
  schedule_id: z.string().uuid({ message: 'ID jadwal tidak valid' }),
  tanggal: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, { message: 'Format tanggal harus YYYY-MM-DD' })
    .optional(),
  open_at: z.string().datetime().optional(),
  close_at: z.string().datetime().optional(),
  late_after: z.string().datetime().optional(),
});

export const getSessionsQuerySchema = z.object({
  page: z.coerce.number().int().positive().default(1),
  pageSize: z.coerce.number().int().positive().max(100).default(20),
  status: z.nativeEnum(SessionStatus).optional(),
  tanggal: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/)
    .optional(),
  class_id: z.string().uuid().optional(),
  schedule_id: z.string().uuid().optional(),
});
