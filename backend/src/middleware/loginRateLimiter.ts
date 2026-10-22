import rateLimit from 'express-rate-limit';
import { Request, Response } from 'express';

export const loginRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 5, // limit each combination of IP + identifier to 5 attempts per windowMs
  keyGenerator: (req: Request): string => {
    const identifier = req.body?.identifier ? String(req.body.identifier).toLowerCase().trim() : '';
    const ip = req.ip || req.socket.remoteAddress || 'unknown';
    return `${ip}:${identifier}`;
  },
  handler: (_req: Request, res: Response): void => {
    res.status(429).json({
      error: {
        code: 'TOO_MANY_ATTEMPTS',
        message: 'Terlalu banyak percobaan login. Silakan coba lagi dalam 15 menit.',
      },
    });
  },
  standardHeaders: true,
  legacyHeaders: false,
});
