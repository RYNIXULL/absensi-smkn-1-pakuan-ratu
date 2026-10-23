import { Router } from 'express';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';
import { ClassesController } from './classes.controller';

export const classesRouter = Router();

classesRouter.use(authenticate);
classesRouter.use(requireRole('operator'));

// Classes CRUD
classesRouter.get('/', ClassesController.list);
classesRouter.post('/', ClassesController.create);
classesRouter.get('/:id', ClassesController.getById);
classesRouter.patch('/:id', ClassesController.update);
classesRouter.delete('/:id', ClassesController.delete);

// Class Members
classesRouter.get('/:id/members', ClassesController.listMembers);
classesRouter.post('/:id/members', ClassesController.addMembers);
classesRouter.delete('/:id/members/:userId', ClassesController.removeMember);
