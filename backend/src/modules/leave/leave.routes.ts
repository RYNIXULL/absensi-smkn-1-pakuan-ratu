import { Router } from 'express';
import multer from 'multer';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';
import { UserRole } from '@prisma/client';
import {
  createLeaveRequest,
  getLeaveAttachment,
  getLeaveRequests,
  reviewLeaveRequest,
} from './leave.controller';
import { AppError } from '../../errors/appError';

const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 5 * 1024 * 1024, // 5 MB max
  },
  fileFilter: (_req, file, cb) => {
    const allowed = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];
    if (allowed.includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new AppError('INVALID_FILE_TYPE', 'Format lampiran harus berupa JPEG, PNG, WEBP, atau PDF', 400));
    }
  },
});

const leaveRouter = Router();

// Siswa: Ajukan izin dengan lampiran (multipart)
leaveRouter.post(
  '/',
  authenticate,
  requireRole(UserRole.siswa),
  upload.single('attachment'),
  createLeaveRequest
);

// Semua role: Daftar izin terfilter (siswa: sendiri, guru: kelas ajar, operator: semua)
leaveRouter.get('/', authenticate, getLeaveRequests);

// Guru / Operator: Setujui atau tolak izin
leaveRouter.patch(
  '/:id',
  authenticate,
  requireRole(UserRole.operator, UserRole.guru),
  reviewLeaveRequest
);

// Signed URL lampiran surat izin
leaveRouter.get('/:id/attachment', authenticate, getLeaveAttachment);

export default leaveRouter;
