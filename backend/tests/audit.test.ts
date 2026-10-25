import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from 'supertest';
import { app } from '../src/app';
import { prisma } from '../src/lib/prisma';
import { signAccessToken } from '../src/lib/token';

vi.mock('../src/lib/prisma', () => {
  return {
    prisma: {
      user: {
        findUnique: vi.fn(),
      },
      auditLog: {
        count: vi.fn(),
        findMany: vi.fn(),
        groupBy: vi.fn(),
      },
    },
  };
});

describe('Audit Logs Module Tests (FR-AUDIT-01)', () => {
  const operatorToken = signAccessToken({ sub: 'user-operator', role: 'operator' });
  const guruToken = signAccessToken({ sub: 'user-guru', role: 'guru' });
  const siswaToken = signAccessToken({ sub: 'user-siswa', role: 'siswa' });

  const mockLogs = [
    {
      id: 'log-1',
      actor_id: 'user-operator',
      action: 'DEVICE_APPROVED',
      entity: 'Device',
      entity_id: 'device-1',
      old_value: null,
      new_value: { status: 'disetujui' },
      created_at: new Date('2026-10-25T08:00:00.000Z'),
      actor: {
        id: 'user-operator',
        nama: 'Operator Utama',
        nomor_induk: 'OP-001',
        role: 'operator',
      },
    },
    {
      id: 'log-2',
      actor_id: 'user-guru',
      action: 'ATTENDANCE_CORRECTED',
      entity: 'attendances',
      entity_id: 'att-1',
      old_value: { status: 'alpa' },
      new_value: { status: 'hadir', catatan: 'Salah input' },
      created_at: new Date('2026-10-25T08:30:00.000Z'),
      actor: {
        id: 'user-guru',
        nama: 'Budi Guru',
        nomor_induk: 'G-002',
        role: 'guru',
      },
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(prisma.user.findUnique).mockImplementation(async ({ where }) => {
      if (where.id === 'user-operator') {
        return {
          id: 'user-operator',
          nama: 'Operator Utama',
          nomor_induk: 'OP-001',
          role: 'operator',
          is_active: true,
        } as any;
      }
      if (where.id === 'user-guru') {
        return {
          id: 'user-guru',
          nama: 'Budi Guru',
          nomor_induk: 'G-002',
          role: 'guru',
          is_active: true,
        } as any;
      }
      if (where.id === 'user-siswa') {
        return {
          id: 'user-siswa',
          nama: 'Siswa Satu',
          nomor_induk: 'S-003',
          role: 'siswa',
          is_active: true,
        } as any;
      }
      return null;
    });
  });

  it('Operator: should get paginated audit logs', async () => {
    vi.mocked(prisma.auditLog.count).mockResolvedValue(2);
    vi.mocked(prisma.auditLog.findMany).mockResolvedValue(mockLogs as any);

    const res = await request(app)
      .get('/api/v1/audit-logs')
      .set('Authorization', `Bearer ${operatorToken}`)
      .query({ page: 1, pageSize: 20 });

    expect(res.status).toBe(200);
    expect(res.body.data).toHaveLength(2);
    expect(res.body.pagination).toBeDefined();
    expect(res.body.pagination.total).toBe(2);
    expect(res.body.data[0].action).toBe('DEVICE_APPROVED');
    expect(res.body.data[0].actor.nama).toBe('Operator Utama');
  });

  it('Operator: should filter audit logs by action, entity, and date', async () => {
    vi.mocked(prisma.auditLog.count).mockResolvedValue(1);
    vi.mocked(prisma.auditLog.findMany).mockResolvedValue([mockLogs[1]] as any);

    const res = await request(app)
      .get('/api/v1/audit-logs')
      .set('Authorization', `Bearer ${operatorToken}`)
      .query({
        action: 'ATTENDANCE_CORRECTED',
        entity: 'attendances',
        start_date: '2026-10-25',
        end_date: '2026-10-25',
      });

    expect(res.status).toBe(200);
    expect(res.body.data).toHaveLength(1);
    expect(res.body.data[0].action).toBe('ATTENDANCE_CORRECTED');
  });

  it('Operator: should get audit statistics', async () => {
    vi.mocked(prisma.auditLog.count)
      .mockResolvedValueOnce(50) // totalLogs
      .mockResolvedValueOnce(12); // todayLogs
    vi.mocked(prisma.auditLog.groupBy).mockResolvedValue([
      { action: 'LOGIN_SUCCESS', _count: { action: 25 } },
      { action: 'DEVICE_APPROVED', _count: { action: 10 } },
    ] as any);

    const res = await request(app)
      .get('/api/v1/audit-logs/stats')
      .set('Authorization', `Bearer ${operatorToken}`);

    expect(res.status).toBe(200);
    expect(res.body.data.totalLogs).toBe(50);
    expect(res.body.data.todayLogs).toBe(12);
    expect(res.body.data.topActions).toHaveLength(2);
    expect(res.body.data.topActions[0].action).toBe('LOGIN_SUCCESS');
  });

  it('Guru: should be forbidden (403)', async () => {
    const res = await request(app)
      .get('/api/v1/audit-logs')
      .set('Authorization', `Bearer ${guruToken}`);

    expect(res.status).toBe(403);
    expect(res.body.error.code).toBe('FORBIDDEN');
  });

  it('Siswa: should be forbidden (403)', async () => {
    const res = await request(app)
      .get('/api/v1/audit-logs')
      .set('Authorization', `Bearer ${siswaToken}`);

    expect(res.status).toBe(403);
    expect(res.body.error.code).toBe('FORBIDDEN');
  });

  it('Unauthenticated: should be rejected (401)', async () => {
    const res = await request(app).get('/api/v1/audit-logs');

    expect(res.status).toBe(401);
  });
});
