import { Router } from 'express';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';
import { validate, validateQuery } from '../../middleware/validate';
import { createManualSessionSchema, getSessionsQuerySchema } from './sessions.schema';
import { sessionsController } from './sessions.controller';
import { submitAttendanceMiddleware } from '../attendance/attendance.routes';

const router = Router();

// Buka sesi manual (operator atau guru kelasnya)
router.post(
  '/',
  authenticate,
  requireRole(['operator', 'guru']),
  validate(createManualSessionSchema),
  sessionsController.openManual
);

// Tutup sesi (operator atau guru kelasnya)
router.patch(
  '/:id/close',
  authenticate,
  requireRole(['operator', 'guru']),
  sessionsController.close
);

// Daftar sesi (terfilter sesuai role)
router.get(
  '/',
  authenticate,
  validateQuery(getSessionsQuerySchema),
  sessionsController.list
);

// Minta challenge nonce (siswa)
router.post(
  '/:id/challenge',
  authenticate,
  requireRole(['siswa']),
  sessionsController.createChallenge
);

// Kirim absensi siswa (multipart)
router.post(
  '/:id/attendance',
  ...submitAttendanceMiddleware
);

export default router;
