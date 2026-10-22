import { Request, Response, NextFunction } from 'express';
import { User } from '@prisma/client';
import { AppError } from '../errors/appError';
import { verifyAccessToken } from '../lib/token';
import { prisma } from '../lib/prisma';

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Express {
    interface Request {
      user?: User;
    }
  }
}

export async function authenticate(
  req: Request,
  _res: Response,
  next: NextFunction,
): Promise<void> {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new AppError('UNAUTHORIZED', 'Token autentikasi diperlukan', 401);
    }

    const token = authHeader.split(' ')[1];
    let payload;
    try {
      payload = verifyAccessToken(token);
    } catch {
      throw new AppError('INVALID_TOKEN', 'Token tidak valid atau telah kedaluwarsa', 401);
    }

    const user = await prisma.user.findUnique({
      where: { id: payload.sub },
    });

    if (!user) {
      throw new AppError('USER_NOT_FOUND', 'Pengguna tidak ditemukan', 401);
    }

    if (!user.is_active) {
      throw new AppError('ACCOUNT_INACTIVE', 'Akun Anda telah dinonaktifkan', 403);
    }

    req.user = user;
    next();
  } catch (error) {
    next(error);
  }
}
