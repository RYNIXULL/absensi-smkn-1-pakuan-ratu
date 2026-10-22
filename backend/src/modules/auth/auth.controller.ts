import { Request, Response, NextFunction } from 'express';
import { authService } from './auth.service';
import {
  changePasswordSchema,
  loginSchema,
  refreshTokenSchema,
} from './auth.schema';
import { AppError } from '../../errors/appError';

export class AuthController {
  async login(req: Request, res: Response, next: NextFunction) {
    try {
      const validated = loginSchema.parse(req.body);
      const result = await authService.login(validated);
      res.status(200).json(result);
    } catch (error) {
      next(error);
    }
  }

  async refresh(req: Request, res: Response, next: NextFunction) {
    try {
      const validated = refreshTokenSchema.parse(req.body);
      const result = await authService.refresh(validated.refreshToken);
      res.status(200).json(result);
    } catch (error) {
      next(error);
    }
  }

  async logout(req: Request, res: Response, next: NextFunction) {
    try {
      if (!req.user) {
        throw new AppError('UNAUTHORIZED', 'Autentikasi diperlukan', 401);
      }
      const rawRefreshToken = req.body?.refreshToken;
      const result = await authService.logout(req.user.id, rawRefreshToken);
      res.status(200).json(result);
    } catch (error) {
      next(error);
    }
  }

  async changePassword(req: Request, res: Response, next: NextFunction) {
    try {
      if (!req.user) {
        throw new AppError('UNAUTHORIZED', 'Autentikasi diperlukan', 401);
      }
      const validated = changePasswordSchema.parse(req.body);
      const result = await authService.changePassword(req.user.id, validated);
      res.status(200).json(result);
    } catch (error) {
      next(error);
    }
  }
}

export const authController = new AuthController();
