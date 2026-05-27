import { Injectable, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { AuditActionResult, AuditLog, AuditSeverity, DateRangeFilter } from '../models/audit.models';
import { AuditApiService } from '../services/audit-api.service';

@Injectable({ providedIn: 'root' })
export class AuditStore {
  private readonly api = inject(AuditApiService);
  private readonly permissions = inject(PermissionStore);

  private readonly auditLogsSignal = signal<readonly AuditLog[]>([]);
  private readonly selectedModuleSignal = signal<string | null>(null);
  private readonly selectedSeveritySignal = signal<AuditSeverity | string | null>(null);
  private readonly selectedActionSignal = signal<string | null>(null);
  private readonly selectedActorSignal = signal<string | null>(null);
  private readonly selectedDateRangeSignal = signal<DateRangeFilter | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly auditLogs = this.auditLogsSignal.asReadonly();
  readonly selectedModule = this.selectedModuleSignal.asReadonly();
  readonly selectedSeverity = this.selectedSeveritySignal.asReadonly();
  readonly selectedAction = this.selectedActionSignal.asReadonly();
  readonly selectedActor = this.selectedActorSignal.asReadonly();
  readonly selectedDateRange = this.selectedDateRangeSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  async loadAuditLogs(workspaceId: string): Promise<AuditActionResult> {
    if (!this.permissions.canViewAuditLogs()) {
      return this.restricted();
    }

    return this.run(async () => {
      const dateRange = this.selectedDateRangeSignal();
      this.auditLogsSignal.set(
        await this.api.getAuditLogs(workspaceId, {
          module: this.selectedModuleSignal(),
          severity: this.selectedSeveritySignal(),
          action: this.selectedActionSignal(),
          actorUserId: this.selectedActorSignal(),
          from: dateRange?.from,
          to: dateRange?.to,
        }),
      );
    });
  }

  setSelectedModule(module: string | null): void {
    this.selectedModuleSignal.set(module);
  }

  setSelectedSeverity(severity: AuditSeverity | string | null): void {
    this.selectedSeveritySignal.set(severity);
  }

  setSelectedAction(action: string | null): void {
    this.selectedActionSignal.set(action);
  }

  setSelectedActor(actor: string | null): void {
    this.selectedActorSignal.set(actor);
  }

  setSelectedDateRange(range: DateRangeFilter | null): void {
    this.selectedDateRangeSignal.set(range);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  private async run(action: () => Promise<void>): Promise<AuditActionResult> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    try {
      await action();
      return { ok: true };
    } catch (error) {
      const message = normalizeHttpError(error).message || 'We could not load audit logs. Please try again.';
      this.errorSignal.set(message);
      return { ok: false, message };
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private restricted(): AuditActionResult {
    const message = 'You do not have access to audit logs.';
    this.errorSignal.set(message);
    return { ok: false, message };
  }
}
