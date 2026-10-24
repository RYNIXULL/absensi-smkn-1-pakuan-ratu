import { Request, Response, NextFunction } from 'express';
import { ExportsService } from './exports.service';
import { exportAttendancesQuerySchema } from './exports.schema';
import { UserRole } from '@prisma/client';

const exportsService = new ExportsService();

export async function exportAttendances(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const query = exportAttendancesQuerySchema.parse(req.query);
    const { buffer, filename } = await exportsService.exportAttendancesToExcel(
      req.user!.id,
      req.user!.role as UserRole,
      query
    );

    res.setHeader(
      'Content-Type',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    );
    res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
    res.setHeader('Content-Length', buffer.length);

    res.status(200).send(buffer);
  } catch (err) {
    next(err);
  }
}
