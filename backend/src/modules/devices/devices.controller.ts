import { Request, Response, NextFunction } from 'express';
import { devicesService } from './devices.service';
import { updateDeviceRequestSchema } from './devices.schema';
import { AppError } from '../../errors/appError';

export class DevicesController {
  async getPendingRequests(_req: Request, res: Response, next: NextFunction) {
    try {
      const requests = await devicesService.getPendingRequests();
      res.status(200).json({ data: requests });
    } catch (error) {
      next(error);
    }
  }

  async reviewRequest(req: Request, res: Response, next: NextFunction) {
    try {
      if (!req.user) {
        throw new AppError('UNAUTHORIZED', 'Autentikasi diperlukan', 401);
      }
      const deviceId = req.params.id;
      const validated = updateDeviceRequestSchema.parse(req.body);
      const result = await devicesService.reviewRequest(deviceId, req.user.id, validated.action);
      res.status(200).json(result);
    } catch (error) {
      next(error);
    }
  }
}

export const devicesController = new DevicesController();
