import { Request, Response, NextFunction } from 'express';
import { usersService } from './users.service';
import { createUserSchema, updateUserSchema, userQuerySchema } from './users.schema';
import { AppError } from '../../errors/appError';

export class UsersController {
  async getMe(req: Request, res: Response, next: NextFunction) {
    try {
      if (!req.user) {
        throw new AppError('UNAUTHORIZED', 'Autentikasi diperlukan', 401);
      }
      const me = await usersService.getMe(req.user.id);
      res.status(200).json(me);
    } catch (error) {
      next(error);
    }
  }

  async getUsers(req: Request, res: Response, next: NextFunction) {
    try {
      const query = userQuerySchema.parse(req.query);
      const result = await usersService.getUsers(query);
      res.status(200).json(result);
    } catch (error) {
      next(error);
    }
  }

  async createUser(req: Request, res: Response, next: NextFunction) {
    try {
      if (!req.user) {
        throw new AppError('UNAUTHORIZED', 'Autentikasi diperlukan', 401);
      }
      const validated = createUserSchema.parse(req.body);
      const result = await usersService.createUser(validated, req.user.id);
      res.status(201).json(result);
    } catch (error) {
      next(error);
    }
  }

  async updateUser(req: Request, res: Response, next: NextFunction) {
    try {
      if (!req.user) {
        throw new AppError('UNAUTHORIZED', 'Autentikasi diperlukan', 401);
      }
      const userId = req.params.id;
      const validated = updateUserSchema.parse(req.body);
      const updated = await usersService.updateUser(userId, validated, req.user.id);
      res.status(200).json(updated);
    } catch (error) {
      next(error);
    }
  }

  async resetPassword(req: Request, res: Response, next: NextFunction) {
    try {
      if (!req.user) {
        throw new AppError('UNAUTHORIZED', 'Autentikasi diperlukan', 401);
      }
      const userId = req.params.id;
      const result = await usersService.resetPassword(userId, req.user.id);
      res.status(200).json(result);
    } catch (error) {
      next(error);
    }
  }
}

export const usersController = new UsersController();
