import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from 'supertest';
import sharp from 'sharp';
import { app } from '../src/app';
import { prisma } from '../src/lib/prisma';
import { signAccessToken } from '../src/lib/token';
import { AttendanceStatus, SessionStatus } from '@prisma/client';
import { getStorageService, LocalStorageService } from '../src/lib/storage';

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
        count: vi.fn(),
      },
      schedule: {
        findMany: vi.fn(),
        findFirst: vi.fn(),
      },
      auditLog: {
        create: vi.fn(),
      },
      $transaction: vi.fn((callback) => callback(prisma)),
    },
  };
});

describe('Attendance System & Error Codes (FR-ABS-01..07)', () => {
  const operatorId = '00000000-0000-0000-0000-000000000001';
  const guruClassOwnerId = '22222222-2222-2222-2222-222222222222';
  const guruOtherId = '33333333-3333-3333-3333-333333333333';
  const studentId = '44444444-4444-4444-4444-444444444444';
  const otherStudentId = '55555555-5555-5555-5555-555555555555';

  const operatorToken = signAccessToken({ sub: operatorId, role: 'operator' });
  const guruOwnerToken = signAccessToken({ sub: guruClassOwnerId, role: 'guru' });
  const guruOtherToken = signAccessToken({ sub: guruOtherId, role: 'guru' });
  const studentToken = signAccessToken({ sub: studentId, role: 'siswa' });
  const otherStudentToken = signAccessToken({ sub: otherStudentId, role: 'siswa' });

  const sessionId = 'aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb';
  const deviceId = 'device-hardware-uuid-12345';
  const validNonce = 'challenge-nonce-random-xyz-987';

  // Base location: Monas Jakarta (-6.175392, 106.827153), radius 100m
  const schoolLat = -6.175392;
  const schoolLng = 106.827153;
  const schoolRadius = 100;

  let validJpegBuffer: Buffer;

  beforeEach(async () => {
    vi.clearAllMocks();

    validJpegBuffer = await sharp({
      create: {
        width: 100,
        height: 100,
        channels: 3,
        background: { r: 200, g: 200, b: 200 },
      },
    })
      .jpeg()
      .toBuffer();

    // Mock prisma.user.findUnique for auth middleware
    vi.mocked(prisma.user.findUnique).mockImplementation(async ({ where }) => {
      const id = where.id;
      if (id === operatorId) return { id: operatorId, role: 'operator', is_active: true } as any;
      if (id === guruClassOwnerId) return { id: guruClassOwnerId, role: 'guru', is_active: true } as any;
      if (id === guruOtherId) return { id: guruOtherId, role: 'guru', is_active: true } as any;
      if (id === studentId) return { id: studentId, role: 'siswa', is_active: true } as any;
      if (id === otherStudentId) return { id: otherStudentId, role: 'siswa', is_active: true } as any;
      return null;
    });
  });

  const getBaseMockSession = (overrides?: Partial<any>) => {
    const now = new Date();
    return {
      id: sessionId,
      status: SessionStatus.terbuka,
      open_at: new Date(now.getTime() - 15 * 60 * 1000), // 15 min ago
      late_after: new Date(now.getTime() + 15 * 60 * 1000), // in 15 min
      close_at: new Date(now.getTime() + 60 * 60 * 1000), // in 60 min
      schedule: {
        id: 'sch-1',
        teacher_id: guruClassOwnerId,
        class: {
          id: 'class-1',
          name: 'X RPL 1',
          members: [{ user_id: studentId }],
        },
        location: {
          id: 'loc-1',
          name: 'Kampus Utama',
          latitude: schoolLat,
          longitude: schoolLng,
          radius_m: schoolRadius,
        },
      },
      ...overrides,
    };
  };

  const setupValidPreconditions = (sessionOverrides?: Partial<any>) => {
    const session = getBaseMockSession(sessionOverrides);
    vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(session as any);

    // Approved device
    vi.mocked(prisma.device.findFirst).mockResolvedValue({
      id: 'dev-db-1',
      user_id: studentId,
      device_id: deviceId,
      is_active: true,
      status: 'disetujui',
    } as any);

    // No existing attendance
    vi.mocked(prisma.attendance.findUnique).mockResolvedValue(null);

    // Valid challenge
    const now = new Date();
    vi.mocked(prisma.attendanceChallenge.findUnique).mockResolvedValue({
      id: 'chal-1',
      user_id: studentId,
      session_id: sessionId,
      nonce: validNonce,
      expires_at: new Date(now.getTime() + 60 * 1000),
      used_at: null,
    } as any);

    // Attendance created
    vi.mocked(prisma.attendance.create).mockImplementation(async ({ data }: any) => {
      return {
        id: 'att-12345',
        ...data,
      };
    });
  };

  describe('Attendance Submission Success & Status (FR-ABS-01, FR-ABS-02)', () => {
    it('should submit successfully with status HADIR when on-time', async () => {
      setupValidPreconditions();

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 12.5)
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'false')
        .attach('selfie', validJpegBuffer, 'selfie.jpg');

      expect(res.status).toBe(201);
      expect(res.body.data.status).toBe('hadir');
      expect(res.body.data.selfie_key).toBeDefined();
      expect(prisma.attendance.create).toHaveBeenCalled();
      expect(prisma.attendanceChallenge.update).toHaveBeenCalled();
    });

    it('should submit successfully with status TERLAMBAT when submitted after late_after', async () => {
      const now = new Date();
      // late_after was 5 minutes ago, but still before close_at
      setupValidPreconditions({
        open_at: new Date(now.getTime() - 30 * 60 * 1000),
        late_after: new Date(now.getTime() - 5 * 60 * 1000),
        close_at: new Date(now.getTime() + 30 * 60 * 1000),
      });

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 10)
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'false')
        .attach('selfie', validJpegBuffer, 'selfie.jpg');

      expect(res.status).toBe(201);
      expect(res.body.data.status).toBe('terlambat');
    });
  });

  describe('Error Code Validations (FR-ABS-03..07 & RND.md Section 6)', () => {
    it('SESSION_CLOSED: should reject when session status is not terbuka', async () => {
      setupValidPreconditions({ status: SessionStatus.ditutup });

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 10)
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'false')
        .attach('selfie', validJpegBuffer, 'selfie.jpg');

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('SESSION_CLOSED');
      expect(prisma.auditLog.create).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            action: 'ATTENDANCE_REJECTED',
            new_value: expect.objectContaining({ code: 'SESSION_CLOSED' }),
          }),
        })
      );
    });

    it('SESSION_CLOSED: should reject when server time is after close_at', async () => {
      const now = new Date();
      setupValidPreconditions({
        open_at: new Date(now.getTime() - 60 * 60 * 1000),
        late_after: new Date(now.getTime() - 40 * 60 * 1000),
        close_at: new Date(now.getTime() - 1000), // closed 1 second ago
      });

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 10)
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'false')
        .attach('selfie', validJpegBuffer, 'selfie.jpg');

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('SESSION_CLOSED');
    });

    it('NOT_IN_CLASS: should reject when student is not member of the class', async () => {
      setupValidPreconditions();
      // Class members list does not contain studentId
      vi.mocked(prisma.attendanceSession.findUnique).mockResolvedValue(
        getBaseMockSession({
          schedule: {
            class: {
              members: [], // not a member
            },
            location: {
              latitude: schoolLat,
              longitude: schoolLng,
              radius_m: schoolRadius,
            },
          },
        }) as any
      );

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 10)
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'false')
        .attach('selfie', validJpegBuffer, 'selfie.jpg');

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('NOT_IN_CLASS');
    });

    it('DEVICE_MISMATCH: should reject when device is not approved for student', async () => {
      setupValidPreconditions();
      // Device lookup returns null (unapproved / wrong device)
      vi.mocked(prisma.device.findFirst).mockResolvedValue(null);

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 10)
        .field('nonce', validNonce)
        .field('device_id', 'unregistered-or-unapproved-device')
        .field('mock_flag', 'false')
        .attach('selfie', validJpegBuffer, 'selfie.jpg');

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('DEVICE_MISMATCH');
    });

    it('ALREADY_CHECKED_IN: should reject when student already attended this session', async () => {
      setupValidPreconditions();
      vi.mocked(prisma.attendance.findUnique).mockResolvedValue({
        id: 'existing-attendance-id',
        session_id: sessionId,
        student_id: studentId,
      } as any);

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 10)
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'false')
        .attach('selfie', validJpegBuffer, 'selfie.jpg');

      expect(res.status).toBe(409);
      expect(res.body.error.code).toBe('ALREADY_CHECKED_IN');
    });

    it('NONCE_INVALID: should reject when nonce is expired or already used', async () => {
      setupValidPreconditions();
      // Expired nonce
      vi.mocked(prisma.attendanceChallenge.findUnique).mockResolvedValue({
        id: 'chal-1',
        user_id: studentId,
        session_id: sessionId,
        nonce: validNonce,
        expires_at: new Date(Date.now() - 5000), // expired 5 seconds ago
        used_at: null,
      } as any);

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 10)
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'false')
        .attach('selfie', validJpegBuffer, 'selfie.jpg');

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('NONCE_INVALID');
    });

    it('MOCK_LOCATION: should reject when mock_flag is true', async () => {
      setupValidPreconditions();

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 10)
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'true') // client reported mock location
        .attach('selfie', validJpegBuffer, 'selfie.jpg');

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('MOCK_LOCATION');
    });

    it('LOW_ACCURACY: should reject when GPS accuracy > 50 meters', async () => {
      setupValidPreconditions();

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 75.0) // 75m > 50m
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'false')
        .attach('selfie', validJpegBuffer, 'selfie.jpg');

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('LOW_ACCURACY');
    });

    it('OUT_OF_RADIUS: should reject when user is outside allowed radius', async () => {
      setupValidPreconditions();
      // Distance from (-6.175392, 106.827153) to (-6.200000, 106.827153) is ~2.7 km (> 100m)
      const distantLat = -6.2;
      const distantLng = 106.827153;

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', distantLat)
        .field('lng', distantLng)
        .field('accuracy', 10)
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'false')
        .attach('selfie', validJpegBuffer, 'selfie.jpg');

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('OUT_OF_RADIUS');
    });

    it('INVALID_SELFIE: should reject when selfie file is missing', async () => {
      setupValidPreconditions();

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 10)
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'false');

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('INVALID_SELFIE');
    });

    it('INVALID_SELFIE: should reject when selfie is non-jpeg (e.g. text or png)', async () => {
      setupValidPreconditions();

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 10)
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'false')
        .attach('selfie', Buffer.from('some text content'), 'selfie.png');

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('INVALID_SELFIE');
    });

    it('INVALID_SELFIE: should reject when selfie file size exceeds 2 MB', async () => {
      setupValidPreconditions();
      const largeBuffer = Buffer.alloc(2.5 * 1024 * 1024); // 2.5 MB

      const res = await request(app)
        .post(`/api/v1/sessions/${sessionId}/attendance`)
        .set('Authorization', `Bearer ${studentToken}`)
        .field('lat', schoolLat)
        .field('lng', schoolLng)
        .field('accuracy', 10)
        .field('nonce', validNonce)
        .field('device_id', deviceId)
        .field('mock_flag', 'false')
        .attach('selfie', largeBuffer, 'selfie.jpg');

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('INVALID_SELFIE');
    });
  });

  describe('Attendance History & Reports (FR-ABS-06)', () => {
    it('Siswa: GET /api/v1/me/attendances should return paginated list of student attendances', async () => {
      vi.mocked(prisma.attendance.findMany).mockResolvedValue([
        {
          id: 'att-1',
          student_id: studentId,
          status: AttendanceStatus.hadir,
          check_in_time: new Date(),
          session: {
            tanggal: new Date(),
            schedule: {
              subject: { name: 'Matematika' },
              class: { name: 'X RPL 1' },
            },
          },
        },
      ] as any);
      vi.mocked(prisma.attendance.count).mockResolvedValue(1);

      const res = await request(app)
        .get('/api/v1/me/attendances?page=1&pageSize=10')
        .set('Authorization', `Bearer ${studentToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toHaveLength(1);
      expect(res.body.pagination.total).toBe(1);
    });

    it('Operator: GET /api/v1/attendances should return all attendances with filters', async () => {
      vi.mocked(prisma.attendance.findMany).mockResolvedValue([]);
      vi.mocked(prisma.attendance.count).mockResolvedValue(0);

      const res = await request(app)
        .get('/api/v1/attendances?status=hadir')
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(200);
      expect(prisma.attendance.findMany).toHaveBeenCalled();
    });

    it('Guru of another class: GET /api/v1/attendances with class_id should be rejected if teacher does not teach that class', async () => {
      const otherClassId = '99999999-9999-9999-9999-999999999999';
      vi.mocked(prisma.schedule.findFirst).mockResolvedValue(null); // teacher has no schedule for this class

      const res = await request(app)
        .get(`/api/v1/attendances?class_id=${otherClassId}`)
        .set('Authorization', `Bearer ${guruOtherToken}`);

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });
  });

  describe('Attendance Correction (FR-ABS-07)', () => {
    const attendanceId = 'att-corr-1111';

    it('Operator: should successfully correct attendance status with required catatan and write audit log', async () => {
      vi.mocked(prisma.attendance.findUnique).mockResolvedValue({
        id: attendanceId,
        status: AttendanceStatus.terlambat,
        catatan: null,
        session: {
          schedule: {
            teacher_id: guruClassOwnerId,
          },
        },
      } as any);

      vi.mocked(prisma.attendance.update).mockResolvedValue({
        id: attendanceId,
        status: AttendanceStatus.hadir,
        catatan: 'Koreksi karena kendala sinyal GPS',
      } as any);

      const res = await request(app)
        .patch(`/api/v1/attendances/${attendanceId}`)
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({
          status: 'hadir',
          catatan: 'Koreksi karena kendala sinyal GPS',
        });

      expect(res.status).toBe(200);
      expect(res.body.data.status).toBe('hadir');
      expect(prisma.auditLog.create).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            action: 'ATTENDANCE_CORRECTED',
            entity_id: attendanceId,
            old_value: expect.objectContaining({ status: 'terlambat' }),
            new_value: expect.objectContaining({ status: 'hadir' }),
          }),
        })
      );
    });

    it('Teacher of another class: should be rejected with 403 FORBIDDEN when attempting correction', async () => {
      vi.mocked(prisma.attendance.findUnique).mockResolvedValue({
        id: attendanceId,
        status: AttendanceStatus.terlambat,
        session: {
          schedule: {
            teacher_id: guruClassOwnerId, // Owner is guruClassOwnerId
          },
        },
      } as any);

      const res = await request(app)
        .patch(`/api/v1/attendances/${attendanceId}`)
        .set('Authorization', `Bearer ${guruOtherToken}`) // Caller is guruOtherId
        .send({
          status: 'hadir',
          catatan: 'Koreksi tanpa izin',
        });

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    it('Should reject correction when catatan is missing (400 INVALID_INPUT)', async () => {
      const res = await request(app)
        .patch(`/api/v1/attendances/${attendanceId}`)
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({
          status: 'hadir',
          // missing catatan
        });

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('VALIDATION_ERROR');
    });
  });

  describe('Signed URL & HMAC File Download Security', () => {
    it('GET /api/v1/attendances/:id/selfie should return a valid signed URL', async () => {
      vi.mocked(prisma.attendance.findUnique).mockResolvedValue({
        id: 'att-1',
        student_id: studentId,
        selfie_key: 'selfies/session-1/student-1.jpg',
        session: {
          schedule: { teacher_id: guruClassOwnerId },
        },
      } as any);

      const res = await request(app)
        .get('/api/v1/attendances/att-1/selfie')
        .set('Authorization', `Bearer ${studentToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data.url).toContain('/api/v1/files/download');
      expect(res.body.data.url).toContain('sig=');
    });

    it('GET /api/v1/files/download should reject tampered signature with 403 FORBIDDEN', async () => {
      const res = await request(app).get(
        '/api/v1/files/download?key=selfies/fake.jpg&expires=9999999999&sig=deadbeef00001111'
      );

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });

    it('GET /api/v1/files/download should reject expired timestamp with 403 FORBIDDEN', async () => {
      const storage = getStorageService() as LocalStorageService;
      const key = 'selfies/test.jpg';
      const pastTime = Math.floor(Date.now() / 1000) - 60; // 60 seconds in the past
      // Generate signature using internal method if possible or standard HMAC
      const sig = (storage as any).generateSignature(key, pastTime);

      const res = await request(app).get(
        `/api/v1/files/download?key=${encodeURIComponent(key)}&expires=${pastTime}&sig=${sig}`
      );

      expect(res.status).toBe(403);
      expect(res.body.error.code).toBe('FORBIDDEN');
    });
  });
});
