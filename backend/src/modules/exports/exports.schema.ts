import { z } from 'zod';
import { AttendanceStatus } from '@prisma/client';

export const exportAttendancesQuerySchema = z.object({
  start_date: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Format start_date harus YYYY-MM-DD')
    .optional(),
  end_date: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Format end_date harus YYYY-MM-DD')
    .optional(),
  class_id: z.string().uuid('Format class_id harus UUID').optional(),
  subject_id: z.string().uuid('Format subject_id harus UUID').optional(),
  student_id: z.string().uuid('Format student_id harus UUID').optional(),
  status: z.nativeEnum(AttendanceStatus).optional(),
});
