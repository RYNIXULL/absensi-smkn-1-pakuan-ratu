import { Request, Response, NextFunction } from 'express';
import { SettingsService } from './settings.service';
import { updateSettingSchema } from './settings.schema';

export class SettingsController {
  static async getAll(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const settings = await SettingsService.getAll();
      res.status(200).json({ data: settings });
    } catch (error) {
      next(error);
    }
  }

  static async update(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const validated = updateSettingSchema.parse(req.body);
      const result = await SettingsService.set(validated.key, validated.value);
      res.status(200).json({ data: result });
    } catch (error) {
      next(error);
    }
  }
}
