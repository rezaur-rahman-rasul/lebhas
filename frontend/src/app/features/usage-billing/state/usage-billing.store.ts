import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  CreditLedger,
  CreditTransactionType,
  DownloadUsageLog,
  MasterAiCostUsage,
  MasterWorkspaceUsage,
  MonthlyUsageSnapshot,
  PlanUtilization,
  ShareUsageLog,
  TopCostWorkspace,
  UsageBillingActionResult,
  UsageBillingFilters,
  UsageBillingLog,
  UsageLimitSnapshot,
  UsageType,
  WorkspaceUsageSummary,
} from '../models/usage-billing.models';
import { UsageBillingApiService } from '../services/usage-billing-api.service';

@Injectable({ providedIn: 'root' })
export class UsageBillingStore {
  private readonly api = inject(UsageBillingApiService);
  private readonly permissions = inject(PermissionStore);
  private readonly notifications = inject(NotificationStateService);

  private readonly usageSummarySignal = signal<WorkspaceUsageSummary | null>(null);
  private readonly currentMonthUsageSignal = signal<WorkspaceUsageSummary | null>(null);
  private readonly creditLedgerSignal = signal<readonly CreditLedger[]>([]);
  private readonly usageBillingLogsSignal = signal<readonly UsageBillingLog[]>([]);
  private readonly downloadUsageSignal = signal<readonly DownloadUsageLog[]>([]);
  private readonly shareUsageSignal = signal<readonly ShareUsageLog[]>([]);
  private readonly monthlySnapshotsSignal = signal<readonly MonthlyUsageSnapshot[]>([]);
  private readonly masterWorkspaceUsageSignal = signal<readonly MasterWorkspaceUsage[]>([]);
  private readonly masterAiCostsSignal = signal<readonly MasterAiCostUsage[]>([]);
  private readonly topCostWorkspacesSignal = signal<readonly TopCostWorkspace[]>([]);
  private readonly planUtilizationSignal = signal<readonly PlanUtilization[]>([]);
  private readonly selectedMonthSignal = signal<string | null>(null);
  private readonly selectedUsageTypeSignal = signal<UsageType | string | null>(null);
  private readonly selectedTransactionTypeSignal = signal<CreditTransactionType | string | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly usageSummary = this.usageSummarySignal.asReadonly();
  readonly currentMonthUsage = this.currentMonthUsageSignal.asReadonly();
  readonly creditLedger = this.creditLedgerSignal.asReadonly();
  readonly usageBillingLogs = this.usageBillingLogsSignal.asReadonly();
  readonly downloadUsage = this.downloadUsageSignal.asReadonly();
  readonly shareUsage = this.shareUsageSignal.asReadonly();
  readonly monthlySnapshots = this.monthlySnapshotsSignal.asReadonly();
  readonly masterWorkspaceUsage = this.masterWorkspaceUsageSignal.asReadonly();
  readonly masterAiCosts = this.masterAiCostsSignal.asReadonly();
  readonly topCostWorkspaces = this.topCostWorkspacesSignal.asReadonly();
  readonly planUtilization = this.planUtilizationSignal.asReadonly();
  readonly selectedMonth = this.selectedMonthSignal.asReadonly();
  readonly selectedUsageType = this.selectedUsageTypeSignal.asReadonly();
  readonly selectedTransactionType = this.selectedTransactionTypeSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  private readonly creditLimit = computed(
    () => this.currentMonthUsageSignal()?.creditLimit ?? this.usageSummarySignal()?.creditLimit ?? null,
  );
  private readonly storageLimit = computed(
    () => this.currentMonthUsageSignal()?.storageLimit ?? this.usageSummarySignal()?.storageLimit ?? null,
  );
  private readonly generatedVersionLimit = computed(
    () =>
      this.currentMonthUsageSignal()?.generatedVersionLimit ??
      this.usageSummarySignal()?.generatedVersionLimit ??
      null,
  );

  readonly availableCredits = computed(() => {
    const limit = this.creditLimit();
    return limit?.remaining ?? this.currentMonthUsageSignal()?.creditLimit?.remaining ?? null;
  });
  readonly creditUsagePercent = computed(() => usagePercent(this.creditLimit()));
  readonly storageUsagePercent = computed(() => usagePercent(this.storageLimit()));
  readonly generatedVersionUsagePercent = computed(() => usagePercent(this.generatedVersionLimit()));
  readonly isCreditLimitNear = computed(() => isNearLimit(this.creditUsagePercent()));
  readonly isStorageLimitNear = computed(() => isNearLimit(this.storageUsagePercent()));
  readonly recentTransactions = computed(() =>
    [...this.creditLedgerSignal()]
      .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
      .slice(0, 10),
  );
  readonly recentUsageLogs = computed(() =>
    [...this.usageBillingLogsSignal()]
      .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
      .slice(0, 10),
  );

  async loadDashboard(workspaceId: string): Promise<UsageBillingActionResult> {
    if (!this.permissions.canViewUsageBilling()) {
      return this.restricted();
    }

    return this.run(async () => {
      const [usageSummary, currentMonthUsage, monthlySnapshots] = await Promise.all([
        this.api.getUsageSummary(workspaceId),
        this.api.getCurrentMonthUsage(workspaceId),
        this.api.getMonthlyUsageSnapshots(workspaceId),
      ]);

      this.usageSummarySignal.set(usageSummary);
      this.currentMonthUsageSignal.set(currentMonthUsage);
      this.monthlySnapshotsSignal.set(monthlySnapshots);
    });
  }

  async loadCreditLedger(workspaceId: string): Promise<UsageBillingActionResult> {
    if (!this.permissions.canViewCreditLedger()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.creditLedgerSignal.set(await this.api.getCreditLedger(workspaceId, this.filters()));
    });
  }

  async loadUsageBillingLogs(workspaceId: string): Promise<UsageBillingActionResult> {
    if (!this.permissions.canViewUsageBilling()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.usageBillingLogsSignal.set(await this.api.getUsageBillingLogs(workspaceId, this.filters()));
    });
  }

  async loadDownloadUsage(workspaceId: string): Promise<UsageBillingActionResult> {
    if (!this.permissions.canViewDownloadUsage()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.downloadUsageSignal.set(await this.api.getDownloadUsage(workspaceId, this.filters()));
    });
  }

  async loadShareUsage(workspaceId: string): Promise<UsageBillingActionResult> {
    if (!this.permissions.canViewShareUsage()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.shareUsageSignal.set(await this.api.getShareUsage(workspaceId, this.filters()));
    });
  }

  async loadMonthlySnapshots(workspaceId: string): Promise<UsageBillingActionResult> {
    if (!this.permissions.canViewUsageBilling()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.monthlySnapshotsSignal.set(await this.api.getMonthlyUsageSnapshots(workspaceId));
    });
  }

  async loadMasterUsageOverview(): Promise<UsageBillingActionResult> {
    if (!this.permissions.canViewMasterUsage()) {
      return this.restricted();
    }

    return this.run(async () => {
      const [workspaceUsage, aiCosts, topCostWorkspaces, planUtilization] = await Promise.all([
        this.api.getMasterWorkspaceUsage(),
        this.api.getMasterAiCosts(),
        this.api.getMasterTopCostWorkspaces(),
        this.api.getMasterPlanUtilization(),
      ]);

      this.masterWorkspaceUsageSignal.set(workspaceUsage);
      this.masterAiCostsSignal.set(aiCosts);
      this.topCostWorkspacesSignal.set(topCostWorkspaces);
      this.planUtilizationSignal.set(planUtilization);
    });
  }

  setSelectedMonth(month: string | null): void {
    this.selectedMonthSignal.set(month);
  }

  setSelectedUsageType(type: UsageType | string | null): void {
    this.selectedUsageTypeSignal.set(type);
  }

  setSelectedTransactionType(type: CreditTransactionType | string | null): void {
    this.selectedTransactionTypeSignal.set(type);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  private filters(): UsageBillingFilters {
    return {
      month: this.selectedMonthSignal(),
      usageType: this.selectedUsageTypeSignal(),
      transactionType: this.selectedTransactionTypeSignal(),
    };
  }

  private async run(action: () => Promise<void>): Promise<UsageBillingActionResult> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      await action();
      return { ok: true };
    } catch (error) {
      const message = friendlyError(error);
      this.errorSignal.set(message);
      this.notifications.error('Usage & Billing', message);
      return { ok: false, message };
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private restricted(): UsageBillingActionResult {
    const message = 'You do not have permission to view usage and billing data.';
    this.errorSignal.set(message);
    return { ok: false, message };
  }
}

function usagePercent(limit: UsageLimitSnapshot | null): number | null {
  if (!limit?.limit || limit.limit <= 0) {
    return null;
  }

  return Math.min(100, Math.max(0, (limit.used / limit.limit) * 100));
}

function isNearLimit(percent: number | null): boolean {
  return typeof percent === 'number' && percent >= 80;
}

function friendlyError(error: unknown): string {
  const normalized = normalizeHttpError(error);
  return normalized.message || 'We could not load usage and billing data. Please try again.';
}
