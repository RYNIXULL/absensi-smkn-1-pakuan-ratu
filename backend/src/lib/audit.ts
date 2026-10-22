import { Prisma, PrismaClient } from '@prisma/client';
import { prisma as defaultPrisma } from './prisma';

export interface AuditLogInput {
  actor_id?: string | null;
  action: string;
  entity: string;
  entity_id?: string | null;
  old_value?: unknown;
  new_value?: unknown;
}

export async function logAudit(
  data: AuditLogInput,
  client: PrismaClient | Prisma.TransactionClient = defaultPrisma,
): Promise<void> {
  try {
    await client.auditLog.create({
      data: {
        actor_id: data.actor_id ?? null,
        action: data.action,
        entity: data.entity,
        entity_id: data.entity_id ?? null,
        old_value: data.old_value ? (data.old_value as Prisma.InputJsonValue) : Prisma.DbNull,
        new_value: data.new_value ? (data.new_value as Prisma.InputJsonValue) : Prisma.DbNull,
      },
    });
  } catch (error) {
    // Audit logging should not crash the main transaction if run outside, but log error
    // eslint-disable-next-line no-console
    console.error('Failed to write audit log:', error);
  }
}
