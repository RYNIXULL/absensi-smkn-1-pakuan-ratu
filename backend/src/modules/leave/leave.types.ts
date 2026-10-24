import { LeaveType, RequestStatus } from '@prisma/client';

export interface CreateLeaveRequestInput {
  session_id: string;
  tipe: LeaveType;
  alasan: string;
}

export interface ReviewLeaveRequestInput {
  status: RequestStatus;
  catatan?: string;
}

export interface LeaveRequestQuery {
  status?: RequestStatus;
  tipe?: LeaveType;
  session_id?: string;
  class_id?: string;
  student_id?: string;
  page?: number;
  pageSize?: number;
}
