import { Request, Response, NextFunction } from 'express';
import { SchedulesService } from './schedules.service';
import {
  createScheduleSchema,
  updateScheduleSchema,
  listScheduleQuerySchema,
} from './schedules.schema';
import { SettingsService } from '../settings/settings.service';
import { AppError } from '../../errors/appError';

export class SchedulesController {
  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const query = listScheduleQuerySchema.parse(req.query);
      const result = await SchedulesService.listSchedules(query);
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
      const schedule = await SchedulesService.getScheduleById(id);
      res.status(200).json({ data: schedule });
    } catch (error) {
      next(error);
    }
  }

  static async create(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const user = req.user!;
      if (user.role === 'guru') {
        const canEdit = (await SettingsService.get('teacher_can_edit_schedule')) === 'true';
        if (!canEdit) {
          throw new AppError('FORBIDDEN', 'Guru tidak diizinkan mengelola jadwal', 403);
        }
        if (req.body.teacher_id && req.body.teacher_id !== user.id) {
          throw new AppError('FORBIDDEN', 'Guru hanya boleh mengelola jadwal untuk dirinya sendiri', 403);
        }
        req.body.teacher_id = user.id;
      }

      const validated = createScheduleSchema.parse(req.body);
      const created = await SchedulesService.createSchedule(validated);
      res.status(201).json({
        data: created,
        message: 'Jadwal berhasil dibuat',
      });
    } catch (error) {
      next(error);
    }
  }

  static async update(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const user = req.user!;
      if (user.role === 'guru') {
        const canEdit = (await SettingsService.get('teacher_can_edit_schedule')) === 'true';
        if (!canEdit) {
          throw new AppError('FORBIDDEN', 'Guru tidak diizinkan mengubah jadwal', 403);
        }
        const current = await SchedulesService.getScheduleById(id);
        if (current.teacher_id !== user.id) {
          throw new AppError('FORBIDDEN', 'Guru hanya boleh mengubah jadwal miliknya sendiri', 403);
        }
      }

      const validated = updateScheduleSchema.parse(req.body);
      const updated = await SchedulesService.updateSchedule(id, validated);
      res.status(200).json({
        data: updated,
        message: 'Jadwal berhasil diperbarui',
      });
    } catch (error) {
      next(error);
    }
  }

  static async delete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const user = req.user!;
      if (user.role === 'guru') {
        const canEdit = (await SettingsService.get('teacher_can_edit_schedule')) === 'true';
        if (!canEdit) {
          throw new AppError('FORBIDDEN', 'Guru tidak diizinkan menghapus jadwal', 403);
        }
        const current = await SchedulesService.getScheduleById(id);
        if (current.teacher_id !== user.id) {
          throw new AppError('FORBIDDEN', 'Guru hanya boleh menghapus jadwal miliknya sendiri', 403);
        }
      }

      await SchedulesService.deleteSchedule(id);
      res.status(200).json({ message: 'Jadwal berhasil dihapus' });
    } catch (error) {
      next(error);
    }
  }

  static async getMe(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const user = req.user!;
      const schedules = await SchedulesService.getMeSchedules(user.id, user.role);
      res.status(200).json({ data: schedules });
    } catch (error) {
      next(error);
    }
  }
}
