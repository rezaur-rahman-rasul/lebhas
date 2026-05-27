import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { UsageEmptyStateComponent } from '../components/usage-empty-state/usage-empty-state';
import { UsageFilterBarComponent } from '../components/usage-filter-bar/usage-filter-bar';
import { UsageLoadingStateComponent } from '../components/usage-loading-state/usage-loading-state';
import { UsageLogCardComponent } from '../components/usage-log-card/usage-log-card';
import { CreditTransactionType, UsageBillingLog, UsageType } from '../models/usage-billing.models';
import { UsageBillingStore } from '../state/usage-billing.store';

@Component({
  selector: 'app-usage-billing-logs-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    RouterLink,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    UsageEmptyStateComponent,
    UsageFilterBarComponent,
    UsageLoadingStateComponent,
    UsageLogCardComponent,
  ],
  templateUrl: './usage-billing-logs.html',
  styleUrl: './usage-billing-logs.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsageBillingLogsPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(UsageBillingStore);

  protected readonly activeWorkspaceId = this.workspace.activeWorkspaceId;
  protected readonly accessDenied = computed(() => !this.permissions.canViewUsageBilling());
  protected readonly logs = this.store.usageBillingLogs;
  protected readonly hasLogs = computed(() => this.logs().length > 0);

  constructor() {
    effect(() => {
      const workspaceId = this.activeWorkspaceId();
      if (!workspaceId || !this.permissions.canViewUsageBilling()) {
        return;
      }

      void this.store.loadUsageBillingLogs(workspaceId);
    });
  }

  protected refresh(): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId || !this.permissions.canViewUsageBilling()) {
      return;
    }

    void this.store.loadUsageBillingLogs(workspaceId);
  }

  protected updateMonth(month: string | null): void {
    this.store.setSelectedMonth(month);
    this.refresh();
  }

  protected updateUsageType(type: UsageType | string | null): void {
    this.store.setSelectedUsageType(type);
    this.refresh();
  }

  protected updateTransactionType(type: CreditTransactionType | string | null): void {
    this.store.setSelectedTransactionType(type);
  }

  protected clearFilters(): void {
    this.store.setSelectedMonth(null);
    this.store.setSelectedUsageType(null);
    this.refresh();
  }

  protected usageLabel(value: string | null): string {
    return toTitleCase(value || 'Usage');
  }

  protected usageTone(log: UsageBillingLog): 'neutral' | 'brand' | 'blue' | 'red' {
    switch (log.usageType) {
      case UsageType.CreativeGeneration:
      case UsageType.GeneratedVersion:
        return 'brand';
      case UsageType.Download:
      case UsageType.PublicShare:
        return 'blue';
      case UsageType.Storage:
        return 'neutral';
      default:
        return log.creditsCharged > 0 ? 'neutral' : 'blue';
    }
  }
}

function toTitleCase(value: string): string {
  return value
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}
