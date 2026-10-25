import { Router } from 'express';
import { UserRole } from '@prisma/client';
import { auditController } from './audit.controller';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';

const router = Router();

router.use(authenticate);
router.use(requireRole(UserRole.operator));

router.get('/', (req, res, next) => auditController.getAuditLogs(req, res, next));
router.get('/stats', (req, res, next) => auditController.getAuditStats(req, res, next));

export const auditRouter = router;
