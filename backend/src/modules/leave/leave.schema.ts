import { z } from 'zod';
import { LeaveType, RequestStatus } from '@prisma/client';

export const createLeaveRequestSchema = z.object({
  session_id: z.string().uuid({ message: 'ID sesi harus berupa UUID valid' }),
  tipe: z.nativeEnum(LeaveType, {
    errorMap: () => ({ message: 'Tipe izin harus salah satu dari: izin, sakit' }),
  }),
  alasan: z
    .string()
    .min(3, { message: 'Alasan pengajuan izin minimal 3 karakter' })
    .max(500, { message: 'Alasan pengajuan izin maksimal 500 karakter' }),
});

export const reviewLeaveRequestSchema = z.object({
  status: z.enum([RequestStatus.disetujui, RequestStatus.ditolak], {
    errorMap: () => ({ message: 'Status review harus disetujui atau ditolak' }),
  }),
  catatan: z.string().max(300).optional(),
});

export const leaveRequestQuerySchema = z.object({
  status: z.nativeEnum(RequestStatus).optional(),
  tipe: z.nativeEnum(LeaveType).optional(),
  session_id: z.string().uuid().optional(),
  class_id: z.string().uuid().optional(),
  student_id: z.string().uuid().optional(),
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(20),
});
