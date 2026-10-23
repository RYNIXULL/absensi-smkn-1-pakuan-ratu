import { z } from 'zod';
import { AttendanceStatus } from '@prisma/client';

export const submitAttendanceBodySchema = z.object({
  lat: z.coerce.number().min(-90).max(90, { message: 'Latitude harus antara -90 dan 90' }),
  lng: z.coerce.number().min(-180).max(180, { message: 'Longitude harus antara -180 dan 180' }),
  accuracy: z.coerce.number().min(0, { message: 'Akurasi tidak boleh negatif' }),
  nonce: z.string().min(1, { message: 'Nonce wajib diisi' }),
  device_id: z.string().min(1, { message: 'ID perangkat wajib diisi' }),
  mock_flag: z.preprocess((val) => {
    if (typeof val === 'string') {
      return val.toLowerCase() === 'true';
    }
    return Boolean(val);
  }, z.boolean()),
  integrity_token: z.string().optional(),
});

export const updateAttendanceCorrectionSchema = z.object({
  status: z.nativeEnum(AttendanceStatus, {
    errorMap: () => ({ message: 'Status absensi tidak valid' }),
  }),
  catatan: z
    .string()
    .min(3, { message: 'Catatan koreksi wajib diisi (minimal 3 karakter)' }),
});

export const getAttendancesQuerySchema = z.object({
  page: z.coerce.number().int().positive().default(1),
  pageSize: z.coerce.number().int().positive().max(100).default(20),
  session_id: z.string().uuid().optional(),
  class_id: z.string().uuid().optional(),
  subject_id: z.string().uuid().optional(),
  status: z.nativeEnum(AttendanceStatus).optional(),
  tanggal: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/)
    .optional(),
});

export const getMyAttendancesQuerySchema = z.object({
  page: z.coerce.number().int().positive().default(1),
  pageSize: z.coerce.number().int().positive().max(100).default(20),
  status: z.nativeEnum(AttendanceStatus).optional(),
  from_date: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/)
    .optional(),
  to_date: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/)
    .optional(),
});
