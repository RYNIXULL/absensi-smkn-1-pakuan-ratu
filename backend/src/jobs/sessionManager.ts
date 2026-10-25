import cron from 'node-cron';
import { prisma } from '../lib/prisma';
import { logger } from '../lib/logger';
import { getJakartaDateInfo, parseTimeToMinutes, createUtcFromJakartaTime } from '../lib/dateUtils';
import { SessionStatus } from '@prisma/client';
import { processAutoAlpa } from './autoAlpa';

import { fcmService } from '../lib/fcm';

export const DEFAULT_OPEN_BEFORE_MIN = 15;
export const DEFAULT_LATE_AFTER_MIN = 15;

/**
 * Idempotent processor for session lifecycle:
 * 1. Creates 'terjadwal' sessions for today's active schedules that don't have one yet.
 * 2. Transitions 'terjadwal' -> 'terbuka' when now() >= open_at and now() < close_at.
 * 3. Transitions 'terbuka' -> 'ditutup' when now() >= close_at.
 */
export async function processSessionLifecycle(currentDate = new Date()): Promise<{
  createdSessions: number;
  openedSessions: number;
  closedSessions: number;
}> {
  const jakartaInfo = getJakartaDateInfo(currentDate);

  // 1. Fetch schedules for today's day of week
  const todaySchedules = await prisma.schedule.findMany({
    where: {
      day_of_week: jakartaInfo.dayOfWeek,
    },
  });

  // 2. Fetch existing sessions for today
  const existingSessions = await prisma.attendanceSession.findMany({
    where: {
      tanggal: jakartaInfo.dateOnly,
    },
    select: {
      schedule_id: true,
    },
  });

  const existingScheduleIds = new Set(existingSessions.map((s) => s.schedule_id));
  const schedulesToCreate = todaySchedules.filter((s) => !existingScheduleIds.has(s.id));

  let createdSessions = 0;
  if (schedulesToCreate.length > 0) {
    const sessionData = schedulesToCreate.map((schedule) => {
      const startMin = parseTimeToMinutes(schedule.start_time);
      const endMin = parseTimeToMinutes(schedule.end_time);

      const openAt = createUtcFromJakartaTime(
        jakartaInfo.dateString,
        startMin - DEFAULT_OPEN_BEFORE_MIN
      );
      const lateAfter = createUtcFromJakartaTime(
        jakartaInfo.dateString,
        startMin + DEFAULT_LATE_AFTER_MIN
      );
      const closeAt = createUtcFromJakartaTime(jakartaInfo.dateString, endMin);

      let initialStatus: SessionStatus = SessionStatus.terjadwal;
      if (currentDate >= openAt && currentDate < closeAt) {
        initialStatus = SessionStatus.terbuka;
      } else if (currentDate >= closeAt) {
        initialStatus = SessionStatus.ditutup;
      }

      return {
        schedule_id: schedule.id,
        tanggal: jakartaInfo.dateOnly,
        open_at: openAt,
        late_after: lateAfter,
        close_at: closeAt,
        status: initialStatus,
      };
    });

    const result = await prisma.attendanceSession.createMany({
      data: sessionData,
      skipDuplicates: true,
    });
    createdSessions = result.count;
    logger.info(
      { count: createdSessions, date: jakartaInfo.dateString },
      'Auto-created attendance sessions for today'
    );
  }

  // 3. Transition 'terjadwal' -> 'terbuka' when open_at <= now < close_at
  const openedResult = await prisma.attendanceSession.updateMany({
    where: {
      status: SessionStatus.terjadwal,
      open_at: { lte: currentDate },
      close_at: { gt: currentDate },
    },
    data: {
      status: SessionStatus.terbuka,
    },
  });

  if (openedResult.count > 0) {
    prisma.attendanceSession
      .findMany({
        where: {
          status: SessionStatus.terbuka,
          open_at: { lte: currentDate },
          close_at: { gt: currentDate },
        },
        select: { id: true },
      })
      .then((sessions) => {
        for (const s of sessions) {
          fcmService.notifyClassSessionOpened(s.id).catch(() => {});
        }
      })
      .catch(() => {});
  }

  // 4. Transition 'terbuka' -> 'ditutup' when close_at <= now
  const closedResult = await prisma.attendanceSession.updateMany({
    where: {
      status: SessionStatus.terbuka,
      close_at: { lte: currentDate },
    },
    data: {
      status: SessionStatus.ditutup,
    },
  });

  // Reconcile auto-alpa for newly closed sessions
  if (closedResult.count > 0) {
    try {
      await processAutoAlpa();
    } catch (err) {
      logger.error({ err }, 'Error running auto-alpa reconciliation during session lifecycle');
    }
  }

  return {
    createdSessions,
    openedSessions: openedResult.count,
    closedSessions: closedResult.count,
  };
}

let cronTask: cron.ScheduledTask | null = null;

export function startSessionCronJob(): void {
  if (cronTask) {
    return;
  }
  // Runs every minute: "* * * * *"
  cronTask = cron.schedule('* * * * *', async () => {
    try {
      await processSessionLifecycle();
    } catch (err) {
      logger.error({ err }, 'Error running session lifecycle cron job');
    }
  });
  logger.info('Attendance session manager cron job started (every minute)');
}

export function stopSessionCronJob(): void {
  if (cronTask) {
    cronTask.stop();
    cronTask = null;
  }
}
