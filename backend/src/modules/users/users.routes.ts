import { Router } from 'express';
import multer from 'multer';
import { UserRole } from '@prisma/client';
import { usersController } from './users.controller';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 }, // 5 MB
});

const router = Router();

// /api/v1/me is accessible to any authenticated user
router.get('/me', authenticate, (req, res, next) => usersController.getMe(req, res, next));

// Operator-only user import routes
router.get(
  '/users/import/template',
  authenticate,
  requireRole(UserRole.operator),
  (req, res, next) => usersController.getImportTemplate(req, res, next)
);

router.post(
  '/users/import',
  authenticate,
  requireRole(UserRole.operator),
  upload.single('file'),
  (req, res, next) => usersController.importUsers(req, res, next)
);

// Operator-only user management routes
router.get('/users', authenticate, requireRole(UserRole.operator), (req, res, next) =>
  usersController.getUsers(req, res, next)
);
router.post('/users', authenticate, requireRole(UserRole.operator), (req, res, next) =>
  usersController.createUser(req, res, next)
);
router.patch('/users/:id', authenticate, requireRole(UserRole.operator), (req, res, next) =>
  usersController.updateUser(req, res, next)
);
router.post('/users/:id/reset-password', authenticate, requireRole(UserRole.operator), (req, res, next) =>
  usersController.resetPassword(req, res, next)
);

export const usersRouter = router;
