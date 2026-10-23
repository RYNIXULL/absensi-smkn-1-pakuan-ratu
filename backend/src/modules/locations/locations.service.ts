import { Prisma } from '@prisma/client';
import { prisma } from '../../lib/prisma';
import { AppError } from '../../errors/appError';
import { CreateLocationInput, UpdateLocationInput, ListLocationQuery } from './locations.schema';

export class LocationsService {
  static async listLocations(query: ListLocationQuery) {
    const { page, pageSize, q, is_active } = query;
    const skip = (page - 1) * pageSize;

    const where: Prisma.LocationWhereInput = {};
    if (q) {
      where.nama = { contains: q, mode: 'insensitive' };
    }
    if (typeof is_active === 'boolean') {
      where.is_active = is_active;
    }

    const [total, items] = await Promise.all([
      prisma.location.count({ where }),
      prisma.location.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: { nama: 'asc' },
        include: {
          _count: {
            select: { schedules: true },
          },
        },
      }),
    ]);

    return {
      items,
      pagination: {
        page,
        pageSize,
        total,
        totalPages: Math.ceil(total / pageSize) || 1,
      },
    };
  }

  static async getLocationById(id: string) {
    const location = await prisma.location.findUnique({
      where: { id },
      include: {
        _count: {
          select: { schedules: true },
        },
      },
    });

    if (!location) {
      throw new AppError('NOT_FOUND', 'Lokasi tidak ditemukan', 404);
    }

    return location;
  }

  static async createLocation(input: CreateLocationInput) {
    return prisma.location.create({
      data: {
        nama: input.nama,
        latitude: input.latitude,
        longitude: input.longitude,
        radius_m: input.radius_m,
        is_active: input.is_active ?? true,
      },
    });
  }

  static async updateLocation(id: string, input: UpdateLocationInput) {
    await this.getLocationById(id);

    return prisma.location.update({
      where: { id },
      data: input,
    });
  }

  static async deleteLocation(id: string) {
    await this.getLocationById(id);

    const schedulesCount = await prisma.schedule.count({
      where: { location_id: id },
    });

    if (schedulesCount > 0) {
      return prisma.location.update({
        where: { id },
        data: { is_active: false },
      });
    }

    return prisma.location.delete({
      where: { id },
    });
  }
}
