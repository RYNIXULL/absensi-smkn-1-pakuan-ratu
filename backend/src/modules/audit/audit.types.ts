export interface AuditLogQuery {
  page?: number;
  pageSize?: number;
  action?: string;
  entity?: string;
  actor_id?: string;
  start_date?: string;
  end_date?: string;
  q?: string;
}

export interface AuditLogActor {
  id: string;
  nama: string;
  nomor_induk: string;
  role: string;
}

export interface AuditLogItem {
  id: string;
  actor_id: string | null;
  actor: AuditLogActor | null;
  action: string;
  entity: string;
  entity_id: string | null;
  old_value: unknown;
  new_value: unknown;
  created_at: Date;
}
