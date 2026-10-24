import { prisma } from '../lib/prisma';
import { logger } from '../lib/logger';
import { AttendanceStatus, LeaveType, RequestStatus, SessionStatus, UserRole } from '@prisma/client';

export interface AutoAlpaResult {
  processedSessions: number;
  alpaCreated: number;
  leaveReconciled: number;
}

/**
 * Auto-Alpa Job (FR-SES-03):
 * When an attendance session is closed ('ditutup'), any student in the session's class
 * who has neither checked in nor has an approved leave request is automatically marked as 'alpa'.
 * If the student has an approved leave request ('disetujui'), marks them as 'izin' or 'sakit'.
 *
 * This function is fully idempotent.
 */
export async function processAutoAlpa(targetSessionId?: string): Promise<AutoAlpaResult> {
  const whereSession = targetSessionId
    ? { id: targetSessionId, status: SessionStatus.ditutup }
    : { status: SessionStatus.ditutup };

  const closedSessions = await prisma.attendanceSession.findMany({
    where: whereSession,
    include: {
      schedule: {
        include: {
          class: {
            include: {
              members: {
                where: {
                  user: {
                    role: UserRole.siswa,
                    is_active: true,
                  },
                },
                select: { user_id: true },
              },
            },
          },
        },
      },
      attendances: {
        select: { student_id: true },
      },
      leave_requests: {
        where: { status: RequestStatus.disetujui },
        select: { student_id: true, tipe: true, alasan: true },
      },
    },
  });

  let totalAlpaCreated = 0;
  let totalLeaveReconciled = 0;

  for (const session of closedSessions) {
    const classMembers = session.schedule.class.members;
    const existingAttendanceStudentIds = new Set(session.attendances.map((a) => a.student_id));
    const approvedLeaveMap = new Map(
      session.leave_requests.map((lr) => [lr.student_id, lr])
    );

    const attendancesToInsert: {
      session_id: string;
      student_id: string;
      status: AttendanceStatus;
      source: string;
      catatan: string;
    }[] = [];

    for (const member of classMembers) {
      if (existingAttendanceStudentIds.has(member.user_id)) {
        continue;
      }

      const approvedLeave = approvedLeaveMap.get(member.user_id);
      if (approvedLeave) {
        attendancesToInsert.push({
          session_id: session.id,
          student_id: member.user_id,
          status:
            approvedLeave.tipe === LeaveType.sakit
              ? AttendanceStatus.sakit
              : AttendanceStatus.izin,
          source: 'izin',
          catatan: approvedLeave.alasan
            ? `[Izin Disetujui] ${approvedLeave.alasan}`
            : 'Izin disetujui',
        });
        totalLeaveReconciled++;
      } else {
        attendancesToInsert.push({
          session_id: session.id,
          student_id: member.user_id,
          status: AttendanceStatus.alpa,
          source: 'system',
          catatan: 'Otomatis alpa saat sesi ditutup',
        });
        totalAlpaCreated++;
      }
    }

    if (attendancesToInsert.length > 0) {
      await prisma.attendance.createMany({
        data: attendancesToInsert,
        skipDuplicates: true,
      });

      logger.info(
        {
          sessionId: session.id,
          alpaCount: attendancesToInsert.filter((a) => a.status === AttendanceStatus.alpa).length,
          leaveCount: attendancesToInsert.filter((a) => a.status !== AttendanceStatus.alpa).length,
        },
        'Auto-alpa & leave reconciliation processed for session'
      );
    }
  }

  return {
    processedSessions: closedSessions.length,
    alpaCreated: totalAlpaCreated,
    leaveReconciled: totalLeaveReconciled,
  };
}
