import { Router } from 'express';
import { exportAttendances } from './exports.controller';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';
import { UserRole } from '@prisma/client';

export const exportsRouter = Router();

exportsRouter.get(
  '/attendances.xlsx',
  authenticate,
  requireRole(UserRole.operator, UserRole.guru),
  exportAttendances
);
