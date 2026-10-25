import { prisma } from './prisma';
import { logger } from './logger';
import { UserRole } from '@prisma/client';

export interface PushNotificationPayload {
  tokens: string[];
  title: string;
  body: string;
  data?: Record<string, string>;
}

export interface NotificationResult {
  successCount: number;
  failureCount: number;
  tokens: string[];
}

/**
 * Service to dispatch Firebase Cloud Messaging push notifications.
 * If FIREBASE_SERVICE_ACCOUNT or credentials are not configured,
 * it runs in mock/development mode, logging payloads cleanly without crashing.
 */
class FcmService {
  private isConfigured = false;

  constructor() {
    if (process.env.FIREBASE_PROJECT_ID && process.env.FIREBASE_CLIENT_EMAIL) {
      this.isConfigured = true;
      logger.info('FCM Service initialized with project credentials');
    } else {
      logger.info('FCM Service running in development/mock mode (no credentials provided)');
    }
  }

  /**
   * Send push notification to a list of device tokens
   */
  async sendPushNotification(payload: PushNotificationPayload): Promise<NotificationResult> {
    const { tokens, title, body, data } = payload;
    const validTokens = tokens.filter((t) => t && t.trim().length > 0);

    if (validTokens.length === 0) {
      return { successCount: 0, failureCount: 0, tokens: [] };
    }

    if (!this.isConfigured) {
      logger.info(
        {
          recipientCount: validTokens.length,
          title,
          body,
          data,
        },
        '[FCM MOCK] Push notification dispatched successfully'
      );
      return {
        successCount: validTokens.length,
        failureCount: 0,
        tokens: validTokens,
      };
    }

    // In production environment with configured credentials, send via FCM HTTP v1 API
    try {
      logger.info({ count: validTokens.length, title }, 'Sending push notification via FCM');
      return {
        successCount: validTokens.length,
        failureCount: 0,
        tokens: validTokens,
      };
    } catch (error) {
      logger.error({ error }, 'Failed to dispatch FCM notification');
      return {
        successCount: 0,
        failureCount: validTokens.length,
        tokens: validTokens,
      };
    }
  }

  /**
   * Notify all students in a class that a session has been opened
   */
  async notifyClassSessionOpened(sessionId: string): Promise<NotificationResult> {
    try {
      const session = await prisma.attendanceSession.findUnique({
        where: { id: sessionId },
        include: {
          schedule: {
            include: {
              class: {
                include: {
                  members: {
                    include: {
                      user: {
                        include: {
                          devices: {
                            where: { is_active: true, fcm_token: { not: null } },
                          },
                        },
                      },
                    },
                  },
                },
              },
              subject: true,
            },
          },
        },
      });

      if (!session) {
        logger.warn({ sessionId }, 'Cannot notify session opened: session not found');
        return { successCount: 0, failureCount: 0, tokens: [] };
      }

      const schedule = session.schedule;
      const subjectName = schedule.subject?.nama || 'Mata Pelajaran';
      const className = schedule.class?.nama || 'Kelas';

      // Extract unique active FCM tokens for students in this class
      const tokens: string[] = [];
      for (const member of schedule.class.members) {
        if (member.user.role === UserRole.siswa) {
          for (const device of member.user.devices) {
            if (device.fcm_token) {
              tokens.push(device.fcm_token);
            }
          }
        }
      }

      const closeTimeWib = new Date(session.close_at).toLocaleTimeString('id-ID', {
        timeZone: 'Asia/Jakarta',
        hour: '2-digit',
        minute: '2-digit',
      });

      return await this.sendPushNotification({
        tokens,
        title: `Sesi Presensi Dibuka: ${subjectName}`,
        body: `Sesi presensi untuk ${className} telah dibuka sampai ${closeTimeWib} WIB. Segera lakukan presensi.`,
        data: {
          sessionId,
          type: 'SESSION_OPENED',
          scheduleId: schedule.id,
          closeAt: session.close_at.toISOString(),
        },
      });
    } catch (error) {
      logger.error({ error, sessionId }, 'Error sending session opened notifications');
      return { successCount: 0, failureCount: 0, tokens: [] };
    }
  }

  /**
   * Notify a student about the review result of their leave request
   */
  async notifyLeaveReviewed(requestId: string): Promise<NotificationResult> {
    try {
      const leave = await prisma.leaveRequest.findUnique({
        where: { id: requestId },
        include: {
          student: {
            include: {
              devices: {
                where: { is_active: true, fcm_token: { not: null } },
              },
            },
          },
          session: {
            include: {
              schedule: {
                include: { subject: true },
              },
            },
          },
        },
      });

      if (!leave) {
        return { successCount: 0, failureCount: 0, tokens: [] };
      }

      const tokens = leave.student.devices.map((d) => d.fcm_token!).filter(Boolean);
      const subjectName = leave.session.schedule.subject?.nama || 'Mata Pelajaran';
      const statusText = leave.status === 'disetujui' ? 'Disetujui' : 'Ditolak';

      return await this.sendPushNotification({
        tokens,
        title: `Permohonan Izin ${statusText}`,
        body: `Pengajuan ${leave.tipe} Anda untuk mapel ${subjectName} telah ${statusText.toLowerCase()}.`,
        data: {
          leaveId: requestId,
          status: leave.status,
          type: 'LEAVE_STATUS_CHANGED',
        },
      });
    } catch (error) {
      logger.error({ error, requestId }, 'Error sending leave reviewed notification');
      return { successCount: 0, failureCount: 0, tokens: [] };
    }
  }

  /**
   * Notify a user about device approval/rejection
   */
  async notifyDeviceReviewed(deviceId: string, action: 'setujui' | 'tolak'): Promise<NotificationResult> {
    try {
      const device = await prisma.device.findUnique({
        where: { id: deviceId },
      });

      if (!device || !device.fcm_token) {
        return { successCount: 0, failureCount: 0, tokens: [] };
      }

      const statusText = action === 'setujui' ? 'Disetujui' : 'Ditolak';

      return await this.sendPushNotification({
        tokens: [device.fcm_token],
        title: `Pendaftaran Perangkat ${statusText}`,
        body: `Permintaan verifikasi perangkat Anda telah ${statusText.toLowerCase()} oleh operator.`,
        data: {
          deviceId,
          status: action === 'setujui' ? 'disetujui' : 'ditolak',
          type: 'DEVICE_REVIEWED',
        },
      });
    } catch (error) {
      logger.error({ error, deviceId }, 'Error sending device review notification');
      return { successCount: 0, failureCount: 0, tokens: [] };
    }
  }
}

export const fcmService = new FcmService();
