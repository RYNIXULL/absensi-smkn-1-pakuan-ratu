import { Router } from 'express';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';
import { LocationsController } from './locations.controller';

export const locationsRouter = Router();

locationsRouter.use(authenticate);
locationsRouter.use(requireRole('operator'));

locationsRouter.get('/', LocationsController.list);
locationsRouter.post('/', LocationsController.create);
locationsRouter.get('/:id', LocationsController.getById);
locationsRouter.patch('/:id', LocationsController.update);
locationsRouter.delete('/:id', LocationsController.delete);
