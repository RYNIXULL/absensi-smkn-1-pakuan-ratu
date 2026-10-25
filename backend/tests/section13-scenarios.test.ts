import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from 'supertest';
import sharp from 'sharp';
import { app } from '../src/app';
import { prisma } from '../src/lib/prisma';
import { signAccessToken } from '../src/lib/token';
import { AttendanceStatus, SessionStatus, RequestStatus, LeaveType } from '@prisma/client';
import { createMockIntegrityToken } from '../src/lib/integrity';

vi.mock('../src/lib/prisma', () => {
  return {
    prisma: {
      user: {
        findUnique: vi.fn(),
      },
      attendanceSession: {
        findUnique: vi.fn(),
      },
      device: {
        findFirst: vi.fn(),
      },
      attendanceChallenge: {
        findUnique: vi.fn(),
        update: vi.fn(),
      },
      attendance: {
        findUnique: vi.fn(),
        findMany: vi.fn(),
        create: vi.fn(),
        update: vi.fn(),
        upsert: vi.fn(),
      },
      schedule: {
        findFirst: vi.fn(),
      },
      leaveRequest: {
        findUnique: vi.fn(),
        findFirst: vi.fn(),
        create: vi.fn(),
        update: vi.fn(),
      },
      auditLog: {
        create: vi.fn(),
      },
      $transaction: vi.fn((callback) => callback(prisma)),
    },
  };
});

describe('Section 13 Mandatory Verification Scenarios (docs/RND.md)', () => {
  const studentId = '11111111-1111-1111-1111-111111111111';
  const guruClassOwnerId = '22222222-2222-2222-2222-222222222222';
  const guruOtherId = '33333333-3333-3333-3333-333333333333';
  const operatorId = '44444444-4444-4444-4444-444444444444';

  const studentToken = signAccessToken({ sub: studentId, role: 'siswa' });
  const guruOwnerToken = signAccessToken({ sub: guruClassOwnerId, role: 'guru' });
  const guruOtherToken = signAccessToken({ sub: guruOtherId, role: 'guru' });
  const operatorToken = signAccessToken({ sub: operatorId, role: 'operator' });

  const sessionId = 'aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb';
  const deviceId = 'device-hardware-uuid-123';
  const validNonce = 'valid-nonce-abc-123';

  // Base location: Monas Jakarta
  const baseLat = -6.175392;
  const baseLng = 106.827153;

  let validSelfieBuffer: Buffer;

  const createValidIntegrityToken = (nonce = validNonce) =>
    createMockIntegrityToken({
      requestDetails: {
        requestPackageName: 'com.absensi.app',
        nonce,
      },
      deviceIntegrity: {
        deviceRecognitionVerdict: ['MEETS_DEVICE_INTEGRITY'],
      },
    });

  beforeEach(async () => {
    vi.clearAllMocks();

    validSelfieBuffer = await sharp({
      create: {
        width: 100,
        height: 100,
        channels: 3,
        background: { r: 100, g: 150, b: 200 },
      },
    })
      .jpeg()
      .toBuffer();

    vi.mocked(prisma.user.findUnique).mockImplementation(async ({ where }) => {
      if (where.id === studentId) {
        return {
          id: studentId,
          nama: 'Siswa Test',
          nomor_induk: 'SISWA-001',
          role: 'siswa',
          is_active: true,
        } as any;
      }
      if (where.id === guruClassOwnerId) {
        return {
          id: guruClassOwnerId,
          nama: 'Guru Pengampu',
          nomor_induk: 'GURU-001',
          role: 'guru',
          is_active: true,
        } as any;
      }
      if (where.id === guruOtherId) {
        return {
          id: guruOtherId,
          nama: 'Guru Lain',
          nomor_induk: 'GURU-002',
          role: 'guru',
          is_active: true,
        } as any;
      }
      if (where.id === operatorId) {
        return {
          id: operatorId,
          nama: 'Operator',
          nomor_induk: 'OP-001',
          role: 'operator',
          is_active: true,
        } as any;
      }
      return null;
    });

    vi.mocked(prisma.device.findFirst).mockResolvedValue({
      id: 'device-record-1',
      user_id: studentId,
      device_id: deviceId,
      is_active: true,
      status: RequestStatus.disetujui,
    } as any);

    vi.mocked(prisma.attendanceChallenge.findUnique).mockResolvedValue({
      id: 'challenge-1',
      user_id: studentId,
      session_id: sessionId,
      nonce: validNonce,
      expires_at: new Date(Date.now() + 60000),
      used_at: null,
    } as any);

    vi.mocked(prisma.attendance.findUnique).mockResolvedValue(null);
    vi.mocked(prisma.leaveRequest.findFirst).mockResolvedValue(null);
  });

  const getMockSession = (overrides: Partial<any> = {}) => ({
    id: sessionId,
    schedule_id: 'sched-1',
    tanggal: new Date(),
    status: SessionStatus.terbuka,
    open_at: new Date(Date.now() - 600000),
    late_after: new Date(Date.now() + 600000),
    close_at: new Date(Date.now() + 1800000),
    schedule: {
      id: 'sched-1',
      teacher_id: guruClassOwnerId,
      location: {
        id: 'loc-1',
        name: 'Monas',
        latitude: baseLat,
        longitude: baseLng,
        radius_m: 50,
      },
      class: {
        id: 'class-1',
        members: [{ user_id: studentId }],
      },
      subject: {
        nama: 'Matematika',
      },
    },
    ...overrides,
  });

  // Skenario 1: Di dalam radius (HADIR)
  it('1. Di dalam radius: presensi berhasil dengan status HADIR', async () => {
    vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(getMockSession() as any);
    vi.mocked(prisma.attendance.create).mockResolvedValue({
      id: 'att-1',
      session_id: sessionId,
      student_id: studentId,
      status: AttendanceStatus.hadir,
      checkin_at: new Date(),
    } as any);

    const token = createValidIntegrityToken();

    const res = await request(app)
      .post(`/api/v1/sessions/${sessionId}/attendance`)
      .set('Authorization', `Bearer ${studentToken}`)
      .field('lat', String(baseLat))
      .field('lng', String(baseLng))
      .field('accuracy', '10')
      .field('nonce', validNonce)
      .field('device_id', deviceId)
      .field('mock_flag', 'false')
      .field('integrity_token', token)
      .attach('selfie', validSelfieBuffer, 'selfie.jpg');

    expect(res.status).toBe(201);
    expect(res.body.data.status).toBe(AttendanceStatus.hadir);
  });

  // Skenario 2: Di luar radius (OUT_OF_RADIUS)
  it('2. Di luar radius: ditolak dengan kode OUT_OF_RADIUS', async () => {
    vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(getMockSession() as any);

    const farLat = baseLat + 0.05; // ~5.5 km away
    const token = createValidIntegrityToken();

    const res = await request(app)
      .post(`/api/v1/sessions/${sessionId}/attendance`)
      .set('Authorization', `Bearer ${studentToken}`)
      .field('lat', String(farLat))
      .field('lng', String(baseLng))
      .field('accuracy', '10')
      .field('nonce', validNonce)
      .field('device_id', deviceId)
      .field('mock_flag', 'false')
      .field('integrity_token', token)
      .attach('selfie', validSelfieBuffer, 'selfie.jpg');

    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe('OUT_OF_RADIUS');
  });

  // Skenario 3: Akurasi buruk / lokasi mati (LOW_ACCURACY)
  it('3. Akurasi GPS buruk (> 50 m): ditolak dengan kode LOW_ACCURACY', async () => {
    vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(getMockSession() as any);
    const token = createValidIntegrityToken();

    const res = await request(app)
      .post(`/api/v1/sessions/${sessionId}/attendance`)
      .set('Authorization', `Bearer ${studentToken}`)
      .field('lat', String(baseLat))
      .field('lng', String(baseLng))
      .field('accuracy', '120') // bad accuracy
      .field('nonce', validNonce)
      .field('device_id', deviceId)
      .field('mock_flag', 'false')
      .field('integrity_token', token)
      .attach('selfie', validSelfieBuffer, 'selfie.jpg');

    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe('LOW_ACCURACY');
  });

  // Skenario 4: Fake GPS / Mock Location aktif (MOCK_LOCATION)
  it('4. Fake GPS aktif: ditolak dengan kode MOCK_LOCATION', async () => {
    vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(getMockSession() as any);
    const token = createValidIntegrityToken();

    const res = await request(app)
      .post(`/api/v1/sessions/${sessionId}/attendance`)
      .set('Authorization', `Bearer ${studentToken}`)
      .field('lat', String(baseLat))
      .field('lng', String(baseLng))
      .field('accuracy', '10')
      .field('nonce', validNonce)
      .field('device_id', deviceId)
      .field('mock_flag', 'true') // Mock location detected on device
      .field('integrity_token', token)
      .attach('selfie', validSelfieBuffer, 'selfie.jpg');

    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe('MOCK_LOCATION');
  });

  // Skenario 5: Perangkat berbeda (DEVICE_MISMATCH)
  it('5. Perangkat berbeda / tidak terikat: ditolak dengan kode DEVICE_MISMATCH', async () => {
    vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(getMockSession() as any);
    vi.mocked(prisma.device.findFirst).mockResolvedValue(null); // Not active on this device
    const token = createValidIntegrityToken();

    const res = await request(app)
      .post(`/api/v1/sessions/${sessionId}/attendance`)
      .set('Authorization', `Bearer ${studentToken}`)
      .field('lat', String(baseLat))
      .field('lng', String(baseLng))
      .field('accuracy', '10')
      .field('nonce', validNonce)
      .field('device_id', 'unregistered-phone-id')
      .field('mock_flag', 'false')
      .field('integrity_token', token)
      .attach('selfie', validSelfieBuffer, 'selfie.jpg');

    expect(res.status).toBe(403);
    expect(res.body.error.code).toBe('DEVICE_MISMATCH');
  });

  // Skenario 6: Absen dua kali (ALREADY_CHECKED_IN)
  it('6. Absen dua kali pada sesi yang sama: ditolak dengan ALREADY_CHECKED_IN', async () => {
    vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(getMockSession() as any);
    vi.mocked(prisma.attendance.findUnique).mockResolvedValue({
      id: 'existing-att-id',
      status: AttendanceStatus.hadir,
    } as any);

    const token = createValidIntegrityToken();

    const res = await request(app)
      .post(`/api/v1/sessions/${sessionId}/attendance`)
      .set('Authorization', `Bearer ${studentToken}`)
      .field('lat', String(baseLat))
      .field('lng', String(baseLng))
      .field('accuracy', '10')
      .field('nonce', validNonce)
      .field('device_id', deviceId)
      .field('mock_flag', 'false')
      .field('integrity_token', token)
      .attach('selfie', validSelfieBuffer, 'selfie.jpg');

    expect(res.status).toBe(409);
    expect(res.body.error.code).toBe('ALREADY_CHECKED_IN');
  });

  // Skenario 7: Sesi sudah ditutup (SESSION_CLOSED)
  it('7. Sesi sudah ditutup: ditolak dengan kode SESSION_CLOSED', async () => {
    vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(
      getMockSession({ status: SessionStatus.ditutup }) as any
    );
    const token = createValidIntegrityToken();

    const res = await request(app)
      .post(`/api/v1/sessions/${sessionId}/attendance`)
      .set('Authorization', `Bearer ${studentToken}`)
      .field('lat', String(baseLat))
      .field('lng', String(baseLng))
      .field('accuracy', '10')
      .field('nonce', validNonce)
      .field('device_id', deviceId)
      .field('mock_flag', 'false')
      .field('integrity_token', token)
      .attach('selfie', validSelfieBuffer, 'selfie.jpg');

    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe('SESSION_CLOSED');
  });

  // Skenario 8: Absen terlambat
  it('8. Absen setelah late_after: tercatat dengan status TERLAMBAT', async () => {
    vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(
      getMockSession({
        late_after: new Date(Date.now() - 300000), // Late_after passed 5 minutes ago
      }) as any
    );
    vi.mocked(prisma.attendance.create).mockResolvedValue({
      id: 'att-late-1',
      session_id: sessionId,
      student_id: studentId,
      status: AttendanceStatus.terlambat,
    } as any);

    const token = createValidIntegrityToken();

    const res = await request(app)
      .post(`/api/v1/sessions/${sessionId}/attendance`)
      .set('Authorization', `Bearer ${studentToken}`)
      .field('lat', String(baseLat))
      .field('lng', String(baseLng))
      .field('accuracy', '10')
      .field('nonce', validNonce)
      .field('device_id', deviceId)
      .field('mock_flag', 'false')
      .field('integrity_token', token)
      .attach('selfie', validSelfieBuffer, 'selfie.jpg');

    expect(res.status).toBe(201);
    expect(res.body.data.status).toBe(AttendanceStatus.terlambat);
  });

  // Skenario 9: Izin diajukan & ditolak/disetujui
  it('9. Alur Izin: Siswa ajukan izin dan Guru meninjau permohonan', async () => {
    vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(getMockSession() as any);
    vi.mocked(prisma.leaveRequest.create).mockResolvedValue({
      id: 'leave-req-1',
      student_id: studentId,
      session_id: sessionId,
      tipe: LeaveType.izin,
      alasan: 'Ada kepentingan keluarga',
      status: RequestStatus.menunggu,
    } as any);

    // 1. Siswa mengajukan izin
    const submitRes = await request(app)
      .post('/api/v1/leave-requests')
      .set('Authorization', `Bearer ${studentToken}`)
      .field('session_id', sessionId)
      .field('tipe', 'izin')
      .field('alasan', 'Ada kepentingan keluarga');

    expect(submitRes.status).toBe(201);
    expect(submitRes.body.data.status).toBe('menunggu');

    // 2. Guru meninjau izin (ditolak)
    vi.mocked(prisma.leaveRequest.findUnique).mockResolvedValue({
      id: 'leave-req-1',
      session_id: sessionId,
      student_id: studentId,
      status: RequestStatus.menunggu,
      tipe: LeaveType.izin,
      alasan: 'Ada kepentingan keluarga',
      session: {
        schedule: {
          teacher_id: guruClassOwnerId,
        },
      },
    } as any);
    vi.mocked(prisma.leaveRequest.update).mockResolvedValue({
      id: 'leave-req-1',
      status: RequestStatus.ditolak,
    } as any);

    const reviewRes = await request(app)
      .patch('/api/v1/leave-requests/leave-req-1')
      .set('Authorization', `Bearer ${guruOwnerToken}`)
      .send({
        status: 'ditolak',
        catatan: 'Alasan kurang jelas',
      });

    expect(reviewRes.status).toBe(200);
    expect(reviewRes.body.data.status).toBe('ditolak');
  });

  // Skenario 10: Unduh Excel oleh guru kelas lain harus ditolak 403
  it('10. Unduh Excel oleh guru kelas lain: ditolak 403 FORBIDDEN', async () => {
    // Class-2 is taught by guruClassOwnerId, but guruOtherId tries to export it
    vi.mocked(prisma.schedule.findFirst).mockResolvedValue(null);

    const res = await request(app)
      .get('/api/v1/exports/attendances.xlsx')
      .set('Authorization', `Bearer ${guruOtherToken}`)
      .query({ class_id: '00000000-0000-0000-0000-000000000999' });

    expect(res.status).toBe(403);
    expect(res.body.error.code).toBe('FORBIDDEN');
  });

  // Skenario 11: Play Integrity gagal (INTEGRITY_FAILED)
  it('11. Play Integrity gagal verifikasi: ditolak dengan INTEGRITY_FAILED', async () => {
    vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(getMockSession() as any);

    // Tampered token
    const invalidToken = 'invalid-tampered-token-xyz';

    const res = await request(app)
      .post(`/api/v1/sessions/${sessionId}/attendance`)
      .set('Authorization', `Bearer ${studentToken}`)
      .field('lat', String(baseLat))
      .field('lng', String(baseLng))
      .field('accuracy', '10')
      .field('nonce', validNonce)
      .field('device_id', deviceId)
      .field('mock_flag', 'false')
      .field('integrity_token', invalidToken)
      .attach('selfie', validSelfieBuffer, 'selfie.jpg');

    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe('INTEGRITY_FAILED');
  });
});
