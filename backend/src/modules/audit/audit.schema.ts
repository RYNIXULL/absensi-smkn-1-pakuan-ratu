import { z } from 'zod';

export const auditLogQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(20),
  action: z.string().optional(),
  entity: z.string().optional(),
  actor_id: z.string().uuid().optional(),
  start_date: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Format start_date harus YYYY-MM-DD')
    .optional(),
  end_date: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Format end_date harus YYYY-MM-DD')
    .optional(),
  q: z.string().optional(),
});

export type AuditLogQueryValidated = z.infer<typeof auditLogQuerySchema>;
