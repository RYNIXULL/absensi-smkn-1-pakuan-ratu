import { Request, Response, NextFunction } from 'express';
import { UserRole } from '@prisma/client';
import { AppError } from '../errors/appError';
import { prisma } from '../lib/prisma';

export function requireRole(...roles: UserRole[]) {
  return (req: Request, _res: Response, next: NextFunction): void => {
    if (!req.user) {
      throw new AppError('UNAUTHORIZED', 'Autentikasi diperlukan', 401);
    }

    if (!roles.includes(req.user.role)) {
      throw new AppError('FORBIDDEN', 'Akses ditolak untuk peran Anda', 403);
    }

    next();
  };
}

export function requireTeacherOfClass(classIdParam = 'classId') {
  return async (req: Request, _res: Response, next: NextFunction): Promise<void> => {
    try {
      if (!req.user) {
        throw new AppError('UNAUTHORIZED', 'Autentikasi diperlukan', 401);
      }

      if (req.user.role === UserRole.operator) {
        // Operator has full access to all classes
        return next();
      }

      if (req.user.role !== UserRole.guru) {
        throw new AppError('FORBIDDEN', 'Hanya guru atau operator yang memiliki akses kelas ini', 403);
      }

      const classId = req.params[classIdParam] || req.body[classIdParam] || req.query[classIdParam];
      if (!classId || typeof classId !== 'string') {
        throw new AppError('BAD_REQUEST', 'Parameter kelas tidak valid', 400);
      }

      // Check if this teacher teaches any schedule in this class
      const teachesClass = await prisma.schedule.findFirst({
        where: {
          class_id: classId,
          teacher_id: req.user.id,
        },
      });

      if (!teachesClass) {
        throw new AppError('FORBIDDEN', 'Anda bukan pengajar pada kelas ini', 403);
      }

      next();
    } catch (error) {
      next(error);
    }
  };
}
