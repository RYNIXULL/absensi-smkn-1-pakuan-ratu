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
      location: {
        findMany: vi.fn(),
        findUnique: vi.fn(),
        count: vi.fn(),
        create: vi.fn(),
        update: vi.fn(),
        delete: vi.fn(),
      },
      schedule: {
        count: vi.fn(),
      },
    },
  };
});

describe('Locations Module Tests', () => {
  const operatorToken = signAccessToken({ sub: 'user-op', role: 'operator' });
  const validLocationId = '55555555-5555-5555-5555-555555555555';

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(prisma.user.findUnique).mockResolvedValue({
      id: 'user-op',
      nama: 'Operator Admin',
      nomor_induk: 'OP001',
      role: 'operator',
      is_active: true,
    } as any);
  });

  describe('POST /api/v1/locations (Validation)', () => {
    it('should create location with valid coordinates and radius', async () => {
      vi.mocked(prisma.location.create).mockResolvedValue({
        id: validLocationId,
        nama: 'Gedung Utama',
        latitude: -6.200000,
        longitude: 106.816666,
        radius_m: 50,
        is_active: true,
      } as any);

      const res = await request(app)
        .post('/api/v1/locations')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({
          nama: 'Gedung Utama',
          latitude: -6.200000,
          longitude: 106.816666,
          radius_m: 50,
        });

      expect(res.status).toBe(201);
      expect(res.body.data.nama).toBe('Gedung Utama');
    });

    it('should reject latitude < -90 or > 90', async () => {
      const res = await request(app)
        .post('/api/v1/locations')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({
          nama: 'Lokasi Salah',
          latitude: 95.0,
          longitude: 100.0,
          radius_m: 50,
        });

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('VALIDATION_ERROR');
    });

    it('should reject longitude < -180 or > 180', async () => {
      const res = await request(app)
        .post('/api/v1/locations')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({
          nama: 'Lokasi Salah',
          latitude: 10.0,
          longitude: 200.0,
          radius_m: 50,
        });

      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('VALIDATION_ERROR');
    });

    it('should reject radius < 10 or > 1000', async () => {
      const resTooSmall = await request(app)
        .post('/api/v1/locations')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({
          nama: 'Lokasi Salah',
          latitude: 10.0,
          longitude: 100.0,
          radius_m: 5,
        });
      expect(resTooSmall.status).toBe(400);

      const resTooBig = await request(app)
        .post('/api/v1/locations')
        .set('Authorization', `Bearer ${operatorToken}`)
        .send({
          nama: 'Lokasi Salah',
          latitude: 10.0,
          longitude: 100.0,
          radius_m: 1500,
        });
      expect(resTooBig.status).toBe(400);
    });
  });

  describe('DELETE /api/v1/locations/:id (Soft-delete vs Hard-delete)', () => {
    it('should soft-delete (set is_active=false) when location is referenced in schedules', async () => {
      vi.mocked(prisma.location.findUnique).mockResolvedValue({
        id: validLocationId,
        nama: 'Lab Komputer',
        is_active: true,
      } as any);
      vi.mocked(prisma.schedule.count).mockResolvedValue(1);
      vi.mocked(prisma.location.update).mockResolvedValue({
        id: validLocationId,
        is_active: false,
      } as any);

      const res = await request(app)
        .delete(`/api/v1/locations/${validLocationId}`)
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(200);
      expect(res.body.message).toContain('dinonaktifkan');
      expect(prisma.location.update).toHaveBeenCalledWith({
        where: { id: validLocationId },
        data: { is_active: false },
      });
    });

    it('should hard-delete when location has no references', async () => {
      vi.mocked(prisma.location.findUnique).mockResolvedValue({
        id: validLocationId,
        nama: 'Lab Komputer',
        is_active: true,
      } as any);
      vi.mocked(prisma.schedule.count).mockResolvedValue(0);
      vi.mocked(prisma.location.delete).mockResolvedValue({} as any);

      const res = await request(app)
        .delete(`/api/v1/locations/${validLocationId}`)
        .set('Authorization', `Bearer ${operatorToken}`);

      expect(res.status).toBe(200);
      expect(res.body.message).toContain('berhasil dihapus');
      expect(prisma.location.delete).toHaveBeenCalledWith({
        where: { id: validLocationId },
      });
    });
  });
});
