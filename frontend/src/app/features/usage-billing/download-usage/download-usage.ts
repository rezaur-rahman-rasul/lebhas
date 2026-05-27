import { DatePipe } from '@angular/common';
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
import { DownloadUsageCardComponent } from '../components/download-usage-card/download-usage-card';
import { UsageEmptyStateComponent } from '../components/usage-empty-state/usage-empty-state';
import { UsageFilterBarComponent } from '../components/usage-filter-bar/usage-filter-bar';
import { UsageLoadingStateComponent } from '../components/usage-loading-state/usage-loading-state';
import { CreditTransactionType, DownloadUsageLog, UsageType } from '../models/usage-billing.models';
import { UsageBillingStore } from '../state/usage-billing.store';

@Component({
  selector: 'app-download-usage-page',
  standalone: true,
  imports: [
    DatePipe,
    RouterLink,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    DownloadUsageCardComponent,
    UsageEmptyStateComponent,
    UsageFilterBarComponent,
    UsageLoadingStateComponent,
  ],
  templateUrl: './download-usage.html',
  styleUrl: './download-usage.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DownloadUsagePage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(UsageBillingStore);

  protected readonly activeWorkspaceId = this.workspace.activeWorkspaceId;
  protected readonly accessDenied = computed(() => !this.permissions.canViewDownloadUsage());
  protected readonly logs = this.store.downloadUsage;
  protected readonly hasLogs = computed(() => this.logs().length > 0);

  constructor() {
    effect(() => {
      const workspaceId = this.activeWorkspaceId();
      if (!workspaceId || !this.permissions.canViewDownloadUsage()) {
        return;
      }

      void this.store.loadDownloadUsage(workspaceId);
    });
  }

  protected refresh(): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId || !this.permissions.canViewDownloadUsage()) {
      return;
    }

    void this.store.loadDownloadUsage(workspaceId);
  }

  protected updateMonth(month: string | null): void {
    this.store.setSelectedMonth(month);
    this.refresh();
  }

  protected updateUsageType(type: UsageType | string | null): void {
    this.store.setSelectedUsageType(type);
  }

  protected updateTransactionType(type: CreditTransactionType | string | null): void {
    this.store.setSelectedTransactionType(type);
  }

  protected clearFilters(): void {
    this.store.setSelectedMonth(null);
    this.refresh();
  }

  protected downloadLabel(value: string | null): string {
    return toTitleCase(value || 'Download');
  }

  protected browserSummary(log: DownloadUsageLog): string {
    return summarizeUserAgent(log.userAgent) ?? 'Device not provided';
  }

  protected accessSummary(log: DownloadUsageLog): string {
    return log.ipAddress ? 'Access location recorded' : 'Access location not provided';
  }
}

function toTitleCase(value: string): string {
  return value
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function summarizeUserAgent(userAgent: string | null): string | null {
  if (!userAgent) {
    return null;
  }

  const lower = userAgent.toLowerCase();
  const browser = lower.includes('firefox')
    ? 'Firefox'
    : lower.includes('edg')
      ? 'Edge'
      : lower.includes('chrome')
        ? 'Chrome'
        : lower.includes('safari')
          ? 'Safari'
          : 'Browser';
  const device = lower.includes('mobile') ? 'Mobile' : 'Desktop';

  return `${device} ${browser}`;
}
