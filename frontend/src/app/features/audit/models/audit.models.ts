export enum AuditSeverity {
  Info = 'INFO',
  Warning = 'WARNING',
  Error = 'ERROR',
  Critical = 'CRITICAL',
}

export interface AuditLog {
  readonly id: string;
  readonly workspaceId: string;
  readonly actorUserId: string | null;
  readonly actorName: string | null;
  readonly actorProfileImageUrl?: string | null;
  readonly action: string;
  readonly module: string;
  readonly referenceType: string | null;
  readonly referenceId: string | null;
  readonly ipAddress: string | null;
  readonly userAgent: string | null;
  readonly metadataJson: string | null;
  readonly severity: AuditSeverity | string;
  readonly createdAt: string;
}

export interface AuditLogFilters {
  readonly module?: string | null;
  readonly severity?: AuditSeverity | string | null;
  readonly action?: string | null;
  readonly actorUserId?: string | null;
  readonly referenceType?: string | null;
  readonly from?: string | null;
  readonly to?: string | null;
  readonly page?: number | null;
  readonly size?: number | null;
}

export interface DateRangeFilter {
  readonly from: string | null;
  readonly to: string | null;
}

export interface AuditActionResult {
  readonly ok: boolean;
  readonly message?: string;
}
