import { Router } from 'express';
import { UserRole } from '@prisma/client';
import { devicesController } from './devices.controller';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';

const router = Router();

router.use(authenticate);
router.use(requireRole(UserRole.operator));

router.get('/requests', (req, res, next) => devicesController.getPendingRequests(req, res, next));
router.patch('/:id', (req, res, next) => devicesController.reviewRequest(req, res, next));

export const devicesRouter = router;
