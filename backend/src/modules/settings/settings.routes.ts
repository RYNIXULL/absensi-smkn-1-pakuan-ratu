import { Router } from 'express';
import { authenticate } from '../../middleware/auth';
import { requireRole } from '../../middleware/rbac';
import { SettingsController } from './settings.controller';

export const settingsRouter = Router();

settingsRouter.use(authenticate);
settingsRouter.get('/', requireRole('operator'), SettingsController.getAll);
settingsRouter.patch('/', requireRole('operator'), SettingsController.update);
