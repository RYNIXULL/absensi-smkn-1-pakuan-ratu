import { Request, Response, NextFunction } from 'express';
import { ClassesService } from './classes.service';
import {
  createClassSchema,
  updateClassSchema,
  listClassQuerySchema,
  addClassMembersSchema,
} from './classes.schema';

export class ClassesController {
  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const query = listClassQuerySchema.parse(req.query);
      const result = await ClassesService.listClasses(query);
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
      const classItem = await ClassesService.getClassById(id);
      res.status(200).json({ data: classItem });
    } catch (error) {
      next(error);
    }
  }

  static async create(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const validated = createClassSchema.parse(req.body);
      const created = await ClassesService.createClass(validated);
      res.status(201).json({
        data: created,
        message: 'Kelas berhasil dibuat',
      });
    } catch (error) {
      next(error);
    }
  }

  static async update(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const validated = updateClassSchema.parse(req.body);
      const updated = await ClassesService.updateClass(id, validated);
      res.status(200).json({
        data: updated,
        message: 'Kelas berhasil diperbarui',
      });
    } catch (error) {
      next(error);
    }
  }

  static async delete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      await ClassesService.deleteClass(id);
      res.status(200).json({ message: 'Kelas berhasil dihapus' });
    } catch (error) {
      next(error);
    }
  }

  static async listMembers(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const members = await ClassesService.listMembers(id);
      res.status(200).json({ data: members });
    } catch (error) {
      next(error);
    }
  }

  static async addMembers(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const validated = addClassMembersSchema.parse(req.body);
      const result = await ClassesService.addMembers(id, validated.user_ids);
      res.status(201).json({
        data: result,
        message: `${result.count} anggota berhasil ditambahkan ke kelas`,
      });
    } catch (error) {
      next(error);
    }
  }

  static async removeMember(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id, userId } = req.params;
      await ClassesService.removeMember(id, userId);
      res.status(200).json({ message: 'Anggota berhasil dikeluarkan dari kelas' });
    } catch (error) {
      next(error);
    }
  }
}
