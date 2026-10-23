import { Request, Response, NextFunction } from 'express';
import { attendanceService } from './attendance.service';

export class AttendanceController {
  async submit(req: Request, res: Response, next: NextFunction) {
    try {
      const sessionId = req.params.id;
      const studentId = req.user!.id;
      const result = await attendanceService.submitAttendance(
        studentId,
        sessionId,
        req.body,
        req.file
      );

      res.status(201).json({
        data: result,
        message: 'Presensi berhasil dicatat',
      });
    } catch (err) {
      next(err);
    }
  }

  async getMyAttendances(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await attendanceService.getMyAttendances(
        req.user!.id,
        req.query as unknown as Parameters<typeof attendanceService.getMyAttendances>[1]
      );
      res.json(result);
    } catch (err) {
      next(err);
    }
  }

  async list(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await attendanceService.getAttendances(
        req.user!.id,
        req.user!.role,
        req.query as unknown as Parameters<typeof attendanceService.getAttendances>[2]
      );
      res.json(result);
    } catch (err) {
      next(err);
    }
  }

  async correct(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await attendanceService.correctAttendance(
        req.params.id,
        req.user!.id,
        req.user!.role,
        req.body
      );
      res.json({
        data: result,
        message: 'Status presensi berhasil dikoreksi',
      });
    } catch (err) {
      next(err);
    }
  }

  async getSelfieUrl(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await attendanceService.getSelfieSignedUrl(
        req.params.id,
        req.user!.id,
        req.user!.role
      );
      res.json({
        data: result,
      });
    } catch (err) {
      next(err);
    }
  }
}

export const attendanceController = new AttendanceController();
