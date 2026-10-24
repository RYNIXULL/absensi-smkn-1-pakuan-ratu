import { AttendanceStatus } from '@prisma/client';

export interface ExportAttendancesQuery {
  start_date?: string;
  end_date?: string;
  class_id?: string;
  subject_id?: string;
  student_id?: string;
  status?: AttendanceStatus;
}
