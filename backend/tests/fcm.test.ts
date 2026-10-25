import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from 'supertest';
import { app } from '../src/app';
import { prisma } from '../src/lib/prisma';
import { signAccessToken } from '../src/lib/token';
import { fcmService } from '../src/lib/fcm';

vi.mock('../src/lib/prisma', () => {
  return {
    prisma: {
      user: {
        findUnique: vi.fn(),
      },
      device: {
        findFirst: vi.fn(),
        findUnique: vi.fn(),
        update: vi.fn(),
      },
      attendanceSession: {
        findUnique: vi.fn(),
      },
      leaveRequest: {
        findUnique: vi.fn(),
      },
    },
  };
});

describe('FCM and Device Token Tests (F7)', () => {
  const siswaToken = signAccessToken({ sub: 'user-siswa', role: 'siswa' });

  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(prisma.user.findUnique).mockResolvedValue({
      id: 'user-siswa',
      nama: 'Siswa Satu',
      nomor_induk: 'S-001',
      role: 'siswa',
      is_active: true,
    } as any);
  });

  describe('POST /api/v1/devices/fcm-token', () => {
    it('should successfully update FCM token for active user device', async () => {
      vi.mocked(prisma.device.findFirst).mockResolvedValue({
        id: 'device-uuid-1',
        user_id: 'user-siswa',
        device_id: 'hardware-android-123',
        is_active: true,
      } as any);

      vi.mocked(prisma.device.update).mockResolvedValue({
        id: 'device-uuid-1',
        device_id: 'hardware-android-123',
        fcm_token: 'fake-fcm-token-xyz',
      } as any);

      const res = await request(app)
        .post('/api/v1/devices/fcm-token')
        .set('Authorization', `Bearer ${siswaToken}`)
        .send({
          device_id: 'hardware-android-123',
          fcm_token: 'fake-fcm-token-xyz',
        });

      expect(res.status).toBe(200);
      expect(res.body.data.message).toContain('berhasil diperbarui');
      expect(res.body.data.device_id).toBe('hardware-android-123');
    });

    it('should return 404 when active device is not found for this user', async () => {
      vi.mocked(prisma.device.findFirst).mockResolvedValue(null);

      const res = await request(app)
        .post('/api/v1/devices/fcm-token')
        .set('Authorization', `Bearer ${siswaToken}`)
        .send({
          device_id: 'unknown-device',
          fcm_token: 'fake-fcm-token-xyz',
        });

      expect(res.status).toBe(404);
      expect(res.body.error.code).toBe('DEVICE_NOT_FOUND');
    });

    it('should return 400 when body input is invalid', async () => {
      const res = await request(app)
        .post('/api/v1/devices/fcm-token')
        .set('Authorization', `Bearer ${siswaToken}`)
        .send({
          device_id: '',
        });

      expect(res.status).toBe(400);
    });
  });

  describe('fcmService Dispatchers', () => {
    it('sendPushNotification: handles empty tokens list gracefully', async () => {
      const result = await fcmService.sendPushNotification({
        tokens: [],
        title: 'Judul',
        body: 'Pesan',
      });

      expect(result.successCount).toBe(0);
      expect(result.failureCount).toBe(0);
    });

    it('notifyClassSessionOpened: dispatches notification to class students', async () => {
      vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue({
        id: 'sess-123',
        close_at: new Date(Date.now() + 3600000),
        schedule: {
          id: 'sched-1',
          class: {
            id: 'cls-1',
            nama: 'XII RPL 1',
            members: [
              {
                user: {
                  id: 'stu-1',
                  role: 'siswa',
                  devices: [
                    {
                      fcm_token: 'token-stu-1',
                      is_active: true,
                    },
                  ],
                },
              },
            ],
          },
          subject: {
            nama: 'Pemrograman Web',
          },
        },
      } as any);

      const result = await fcmService.notifyClassSessionOpened('sess-123');
      expect(result.successCount).toBe(1);
      expect(result.tokens).toContain('token-stu-1');
    });
  });
});
