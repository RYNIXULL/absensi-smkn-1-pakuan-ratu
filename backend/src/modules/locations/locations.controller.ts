import { Request, Response, NextFunction } from 'express';
import { LocationsService } from './locations.service';
import {
  createLocationSchema,
  updateLocationSchema,
  listLocationQuerySchema,
} from './locations.schema';

export class LocationsController {
  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const query = listLocationQuerySchema.parse(req.query);
      const result = await LocationsService.listLocations(query);
      res.status(200).json({
        data: result.items,
        pagination: result.pagination,
      });
    } catch (error) {
      next(error);
    }
  }

  static async getById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const location = await LocationsService.getLocationById(id);
      res.status(200).json({ data: location });
    } catch (error) {
      next(error);
    }
  }

  static async create(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const validated = createLocationSchema.parse(req.body);
      const created = await LocationsService.createLocation(validated);
      res.status(201).json({
        data: created,
        message: 'Lokasi berhasil ditambahkan',
      });
    } catch (error) {
      next(error);
    }
  }

  static async update(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const validated = updateLocationSchema.parse(req.body);
      const updated = await LocationsService.updateLocation(id, validated);
      res.status(200).json({
        data: updated,
        message: 'Lokasi berhasil diperbarui',
      });
    } catch (error) {
      next(error);
    }
  }

  static async delete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const result = await LocationsService.deleteLocation(id);
      const isSoftDeleted = 'is_active' in result && result.is_active === false;
      res.status(200).json({
        message: isSoftDeleted
          ? 'Lokasi dinonaktifkan karena masih terhubung ke jadwal'
          : 'Lokasi berhasil dihapus',
      });
    } catch (error) {
      next(error);
    }
  }
}
