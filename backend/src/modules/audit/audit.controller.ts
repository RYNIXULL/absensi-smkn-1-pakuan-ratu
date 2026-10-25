import { Request, Response, NextFunction } from 'express';
import { auditService } from './audit.service';
import { auditLogQuerySchema } from './audit.schema';

export class AuditController {
  async getAuditLogs(req: Request, res: Response, next: NextFunction) {
    try {
      const validated = auditLogQuerySchema.parse(req.query);
      const result = await auditService.getAuditLogs(validated);
      res.status(200).json(result);
    } catch (error) {
      next(error);
    }
  }

  async getAuditStats(_req: Request, res: Response, next: NextFunction) {
    try {
      const stats = await auditService.getAuditStats();
      res.status(200).json({ data: stats });
    } catch (error) {
      next(error);
    }
  }
}

export const auditController = new AuditController();
