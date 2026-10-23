import { z } from 'zod';

const timeRegex = /^([01]\d|2[0-3]):([0-5]\d)(:([0-5]\d))?$/;

export const createScheduleSchema = z
  .object({
    class_id: z.string().uuid('ID kelas harus berupa UUID valid'),
    subject_id: z.string().uuid('ID mapel harus berupa UUID valid'),
    teacher_id: z.string().uuid('ID guru harus berupa UUID valid'),
    location_id: z.string().uuid('ID lokasi harus berupa UUID valid'),
    day_of_week: z.coerce.number().int().min(1, 'Hari minimal 1 (Senin)').max(7, 'Hari maksimal 7 (Minggu)'),
    start_time: z.string().regex(timeRegex, 'Format jam mulai harus HH:mm atau HH:mm:ss'),
    end_time: z.string().regex(timeRegex, 'Format jam selesai harus HH:mm atau HH:mm:ss'),
  })
  .refine((data) => data.start_time < data.end_time, {
    message: 'Jam mulai harus lebih awal daripada jam selesai',
    path: ['start_time'],
  });

export const updateScheduleSchema = z
  .object({
    class_id: z.string().uuid('ID kelas harus berupa UUID valid').optional(),
    subject_id: z.string().uuid('ID mapel harus berupa UUID valid').optional(),
    teacher_id: z.string().uuid('ID guru harus berupa UUID valid').optional(),
    location_id: z.string().uuid('ID lokasi harus berupa UUID valid').optional(),
    day_of_week: z.coerce.number().int().min(1).max(7).optional(),
    start_time: z.string().regex(timeRegex, 'Format jam mulai harus HH:mm atau HH:mm:ss').optional(),
    end_time: z.string().regex(timeRegex, 'Format jam selesai harus HH:mm atau HH:mm:ss').optional(),
  })
  .refine(
    (data) => {
      if (data.start_time && data.end_time) {
        return data.start_time < data.end_time;
      }
      return true;
    },
    {
      message: 'Jam mulai harus lebih awal daripada jam selesai',
      path: ['start_time'],
    }
  );

export const listScheduleQuerySchema = z.object({
  page: z.coerce.number().int().positive().default(1),
  pageSize: z.coerce.number().int().positive().max(100).default(20),
  class_id: z.string().uuid().optional(),
  teacher_id: z.string().uuid().optional(),
  day_of_week: z.coerce.number().int().min(1).max(7).optional(),
  q: z.string().optional(),
});

export type CreateScheduleInput = z.infer<typeof createScheduleSchema>;
export type UpdateScheduleInput = z.infer<typeof updateScheduleSchema>;
export type ListScheduleQuery = z.infer<typeof listScheduleQuerySchema>;
