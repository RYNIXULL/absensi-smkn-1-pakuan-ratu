import { RequestStatus } from '@prisma/client';
import { prisma } from '../../lib/prisma';
import { AppError } from '../../errors/appError';
import { logAudit } from '../../lib/audit';
import { fcmService } from '../../lib/fcm';
import { UpdateFcmTokenInput } from './devices.schema';

export class DevicesService {
  async getPendingRequests() {
    return prisma.device.findMany({
      where: { status: RequestStatus.menunggu },
      include: {
        user: {
          select: {
            id: true,
            nama: true,
            nomor_induk: true,
            role: true,
          },
        },
      },
      orderBy: { created_at: 'desc' },
    });
  }

  async reviewRequest(deviceId: string, operatorId: string, action: 'setujui' | 'tolak') {
    const device = await prisma.device.findUnique({
      where: { id: deviceId },
    });

    if (!device) {
      throw new AppError('DEVICE_NOT_FOUND', 'Permintaan perangkat tidak ditemukan', 404);
    }

    if (device.status !== RequestStatus.menunggu) {
      throw new AppError('ALREADY_PROCESSED', 'Permintaan perangkat telah diproses sebelumnya', 400);
    }

    if (action === 'setujui') {
      await prisma.$transaction([
        // Deactivate all previous devices of this user
        prisma.device.updateMany({
          where: {
            user_id: device.user_id,
            id: { not: device.id },
          },
          data: {
            is_active: false,
          },
        }),
        // Activate and approve this device
        prisma.device.update({
          where: { id: device.id },
          data: {
            status: RequestStatus.disetujui,
            is_active: true,
          },
        }),
      ]);

      await logAudit({
        actor_id: operatorId,
        action: 'DEVICE_APPROVED',
        entity: 'Device',
        entity_id: device.id,
        new_value: { user_id: device.user_id, device_id: device.device_id, status: 'disetujui' },
      });

      // Dispatch FCM push notification asynchronously
      fcmService.notifyDeviceReviewed(device.id, 'setujui').catch(() => {});

      return { message: 'Perangkat berhasil disetujui' };
    } else {
      await prisma.device.update({
        where: { id: device.id },
        data: {
          status: RequestStatus.ditolak,
          is_active: false,
        },
      });

      await logAudit({
        actor_id: operatorId,
        action: 'DEVICE_REJECTED',
        entity: 'Device',
        entity_id: device.id,
        new_value: { user_id: device.user_id, device_id: device.device_id, status: 'ditolak' },
      });

      // Dispatch FCM push notification asynchronously
      fcmService.notifyDeviceReviewed(device.id, 'tolak').catch(() => {});

      return { message: 'Perangkat berhasil ditolak' };
    }
  }

  async updateFcmToken(userId: string, input: UpdateFcmTokenInput) {
    const device = await prisma.device.findFirst({
      where: {
        user_id: userId,
        device_id: input.device_id,
        is_active: true,
      },
    });

    if (!device) {
      throw new AppError('DEVICE_NOT_FOUND', 'Perangkat aktif tidak ditemukan untuk akun ini', 404);
    }

    const updated = await prisma.device.update({
      where: { id: device.id },
      data: { fcm_token: input.fcm_token },
    });

    return {
      message: 'FCM token berhasil diperbarui',
      device_id: updated.device_id,
    };
  }
}

export const devicesService = new DevicesService();
