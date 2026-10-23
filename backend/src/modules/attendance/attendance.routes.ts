import { Router } from 'express';
import multer from 'multer';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';
import { validate, validateQuery } from '../../middleware/validate';
import {
  submitAttendanceBodySchema,
  updateAttendanceCorrectionSchema,
  getAttendancesQuerySchema,
  getMyAttendancesQuerySchema,
} from './attendance.schema';
import { attendanceController } from './attendance.controller';

const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 10 * 1024 * 1024, // Handled inside service for exact 2MB error code & audit log
  },
});

const router = Router();

// GET /attendances - Rekap absensi (operator & guru)
router.get(
  '/',
  authenticate,
  requireRole(['operator', 'guru']),
  validateQuery(getAttendancesQuerySchema),
  attendanceController.list
);

// PATCH /attendances/:id - Koreksi manual status absensi (operator & guru kelasnya)
router.patch(
  '/:id',
  authenticate,
  requireRole(['operator', 'guru']),
  validate(updateAttendanceCorrectionSchema),
  attendanceController.correct
);

// GET /attendances/:id/selfie - Ambil signed URL foto selfie (operator, guru, siswa pemilik)
router.get(
  '/:id/selfie',
  authenticate,
  attendanceController.getSelfieUrl
);

export default router;

// Router khusus untuk /me/attendances
export const myAttendanceRouter = Router();
myAttendanceRouter.get(
  '/',
  authenticate,
  requireRole(['siswa']),
  validateQuery(getMyAttendancesQuerySchema),
  attendanceController.getMyAttendances
);

// Handler untuk POST /sessions/:id/attendance
export const submitAttendanceMiddleware = [
  authenticate,
  requireRole(['siswa']),
  upload.single('selfie'),
  validate(submitAttendanceBodySchema),
  attendanceController.submit,
];
