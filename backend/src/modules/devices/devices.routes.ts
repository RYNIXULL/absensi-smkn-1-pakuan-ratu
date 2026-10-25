import { Router } from 'express';
import { UserRole } from '@prisma/client';
import { devicesController } from './devices.controller';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';

const router = Router();

router.use(authenticate);

// Update FCM Token (semua role yang login)
router.post('/fcm-token', (req, res, next) => devicesController.updateFcmToken(req, res, next));

// Operator-only endpoints
router.get('/requests', requireRole(UserRole.operator), (req, res, next) =>
  devicesController.getPendingRequests(req, res, next)
);
router.patch('/:id', requireRole(UserRole.operator), (req, res, next) =>
  devicesController.reviewRequest(req, res, next)
);

export const devicesRouter = router;
