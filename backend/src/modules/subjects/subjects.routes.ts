import { Router } from 'express';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';
import { SubjectsController } from './subjects.controller';

export const subjectsRouter = Router();

subjectsRouter.use(authenticate);
subjectsRouter.use(requireRole('operator'));

subjectsRouter.get('/', SubjectsController.list);
subjectsRouter.post('/', SubjectsController.create);
subjectsRouter.get('/:id', SubjectsController.getById);
subjectsRouter.patch('/:id', SubjectsController.update);
subjectsRouter.delete('/:id', SubjectsController.delete);
