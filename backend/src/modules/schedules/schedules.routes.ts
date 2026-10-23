import { Router } from 'express';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';
import { SchedulesController } from './schedules.controller';

export const schedulesRouter = Router();

schedulesRouter.use(authenticate);

// Endpoint pribadi guru & siswa
schedulesRouter.get('/me/schedules', requireRole('guru', 'siswa', 'operator'), SchedulesController.getMe);

// Master schedules CRUD (operator dan guru bila diizinkan di controller)
schedulesRouter.get('/schedules', requireRole('operator', 'guru'), SchedulesController.list);
schedulesRouter.post('/schedules', requireRole('operator', 'guru'), SchedulesController.create);
schedulesRouter.get('/schedules/:id', requireRole('operator', 'guru'), SchedulesController.getById);
schedulesRouter.patch('/schedules/:id', requireRole('operator', 'guru'), SchedulesController.update);
schedulesRouter.delete('/schedules/:id', requireRole('operator', 'guru'), SchedulesController.delete);
