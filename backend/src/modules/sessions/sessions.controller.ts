import { Request, Response, NextFunction } from 'express';
import { sessionsService } from './sessions.service';

export class SessionsController {
  async openManual(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await sessionsService.openManualSession(
        req.user!.id,
        req.user!.role,
        req.body
      );
      res.status(201).json({
        data: result,
        message: 'Sesi absensi berhasil dibuka',
      });
    } catch (err) {
      next(err);
    }
  }

  async close(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await sessionsService.closeSession(
        req.params.id,
        req.user!.id,
        req.user!.role
      );
      res.json({
        data: result,
        message: 'Sesi absensi berhasil ditutup',
      });
    } catch (err) {
      next(err);
    }
  }

  async list(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await sessionsService.getSessions(
        req.user!.id,
        req.user!.role,
        req.query as unknown as Parameters<typeof sessionsService.getSessions>[2]
      );
      res.json(result);
    } catch (err) {
      next(err);
    }
  }

  async createChallenge(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await sessionsService.createChallenge(
        req.params.id,
        req.user!.id
      );
      res.status(201).json({
        data: result,
        message: 'Challenge nonce berhasil dibuat',
      });
    } catch (err) {
      next(err);
    }
  }
}

export const sessionsController = new SessionsController();
