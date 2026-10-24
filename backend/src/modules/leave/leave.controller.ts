import { Request, Response, NextFunction } from 'express';
import { LeaveService } from './leave.service';
import {
  createLeaveRequestSchema,
  leaveRequestQuerySchema,
  reviewLeaveRequestSchema,
} from './leave.schema';
import { UserRole } from '@prisma/client';

const leaveService = new LeaveService();

export async function createLeaveRequest(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const validated = createLeaveRequestSchema.parse(req.body);
    const result = await leaveService.createLeaveRequest(
      req.user!.id,
      validated,
      req.file
    );
    res.status(201).json({
      message: 'Pengajuan izin berhasil dikirim',
      data: result,
    });
  } catch (err) {
    next(err);
  }
}

export async function getLeaveRequests(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const query = leaveRequestQuerySchema.parse(req.query);
    const result = await leaveService.getLeaveRequests(
      { id: req.user!.id, role: req.user!.role as UserRole },
      query
    );
    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
}

export async function reviewLeaveRequest(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const validated = reviewLeaveRequestSchema.parse(req.body);
    const result = await leaveService.reviewLeaveRequest(
      req.user!.id,
      req.user!.role as UserRole,
      req.params.id,
      validated
    );
    res.status(200).json({
      message: `Pengajuan izin berhasil ${validated.status}`,
      data: result,
    });
  } catch (err) {
    next(err);
  }
}

export async function getLeaveAttachment(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const result = await leaveService.getAttachmentSignedUrl(
      { id: req.user!.id, role: req.user!.role as UserRole },
      req.params.id
    );
    res.status(200).json({
      data: result,
    });
  } catch (err) {
    next(err);
  }
}
