import { Request, Response, NextFunction } from 'express';
import { SubjectsService } from './subjects.service';
import {
  createSubjectSchema,
  updateSubjectSchema,
  listSubjectQuerySchema,
} from './subjects.schema';

export class SubjectsController {
  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const query = listSubjectQuerySchema.parse(req.query);
      const result = await SubjectsService.listSubjects(query);
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
      const subject = await SubjectsService.getSubjectById(id);
      res.status(200).json({ data: subject });
    } catch (error) {
      next(error);
    }
  }

  static async create(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const validated = createSubjectSchema.parse(req.body);
      const created = await SubjectsService.createSubject(validated);
      res.status(201).json({
        data: created,
        message: 'Mata pelajaran berhasil ditambahkan',
      });
    } catch (error) {
      next(error);
    }
  }

  static async update(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const validated = updateSubjectSchema.parse(req.body);
      const updated = await SubjectsService.updateSubject(id, validated);
      res.status(200).json({
        data: updated,
        message: 'Mata pelajaran berhasil diperbarui',
      });
    } catch (error) {
      next(error);
    }
  }

  static async delete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      await SubjectsService.deleteSubject(id);
      res.status(200).json({ message: 'Mata pelajaran berhasil dihapus' });
    } catch (error) {
      next(error);
    }
  }
}
