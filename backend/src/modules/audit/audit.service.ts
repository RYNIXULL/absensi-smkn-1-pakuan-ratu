import { Prisma } from '@prisma/client';
import { prisma } from '../../lib/prisma';
import { AuditLogQueryValidated } from './audit.schema';

export class AuditService {
  async getAuditLogs(query: AuditLogQueryValidated) {
    const { page, pageSize, action, entity, actor_id, start_date, end_date, q } = query;
    const skip = (page - 1) * pageSize;

    const where: Prisma.AuditLogWhereInput = {};

    if (action) {
      where.action = { contains: action, mode: 'insensitive' };
    }

    if (entity) {
      where.entity = { contains: entity, mode: 'insensitive' };
    }

    if (actor_id) {
      where.actor_id = actor_id;
    }

    if (start_date || end_date) {
      where.created_at = {};
      if (start_date) {
        where.created_at.gte = new Date(`${start_date}T00:00:00.000Z`);
      }
      if (end_date) {
        where.created_at.lte = new Date(`${end_date}T23:59:59.999Z`);
      }
    }

    if (q && q.trim().length > 0) {
      const keyword = q.trim();
      where.OR = [
        { action: { contains: keyword, mode: 'insensitive' } },
        { entity: { contains: keyword, mode: 'insensitive' } },
        {
          actor: {
            nama: { contains: keyword, mode: 'insensitive' },
          },
        },
      ];
    }

    const [total, logs] = await Promise.all([
      prisma.auditLog.count({ where }),
      prisma.auditLog.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: { created_at: 'desc' },
        include: {
          actor: {
            select: {
              id: true,
              nama: true,
              nomor_induk: true,
              role: true,
            },
          },
        },
      }),
    ]);

    return {
      data: logs,
      pagination: {
        page,
        pageSize,
        total,
        totalPages: Math.ceil(total / pageSize) || 1,
      },
    };
  }

  async getAuditStats() {
    const now = new Date();
    const todayStart = new Date(now.toISOString().split('T')[0] + 'T00:00:00.000Z');

    const [totalLogs, todayLogs, topActions] = await Promise.all([
      prisma.auditLog.count(),
      prisma.auditLog.count({
        where: {
          created_at: { gte: todayStart },
        },
      }),
      prisma.auditLog.groupBy({
        by: ['action'],
        _count: { action: true },
        orderBy: { _count: { action: 'desc' } },
        take: 5,
      }),
    ]);

    return {
      totalLogs,
      todayLogs,
      topActions: topActions.map((item) => ({
        action: item.action,
        count: item._count.action,
      })),
    };
  }
}

export const auditService = new AuditService();
