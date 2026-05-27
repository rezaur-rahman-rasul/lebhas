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
import { ShareUsageCardComponent } from '../components/share-usage-card/share-usage-card';
import { UsageEmptyStateComponent } from '../components/usage-empty-state/usage-empty-state';
import { UsageFilterBarComponent } from '../components/usage-filter-bar/usage-filter-bar';
import { UsageLoadingStateComponent } from '../components/usage-loading-state/usage-loading-state';
import { CreditTransactionType, ShareUsageLog, UsageType } from '../models/usage-billing.models';
import { UsageBillingStore } from '../state/usage-billing.store';

@Component({
  selector: 'app-share-usage-page',
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
    ShareUsageCardComponent,
    UsageEmptyStateComponent,
    UsageFilterBarComponent,
    UsageLoadingStateComponent,
  ],
  templateUrl: './share-usage.html',
  styleUrl: './share-usage.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShareUsagePage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(UsageBillingStore);

  protected readonly activeWorkspaceId = this.workspace.activeWorkspaceId;
  protected readonly accessDenied = computed(() => !this.permissions.canViewShareUsage());
  protected readonly logs = this.store.shareUsage;
  protected readonly hasLogs = computed(() => this.logs().length > 0);
  protected readonly accessCount = computed(() => this.logs().length);

  constructor() {
    effect(() => {
      const workspaceId = this.activeWorkspaceId();
      if (!workspaceId || !this.permissions.canViewShareUsage()) {
        return;
      }

      void this.store.loadShareUsage(workspaceId);
    });
  }

  protected refresh(): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId || !this.permissions.canViewShareUsage()) {
      return;
    }

    void this.store.loadShareUsage(workspaceId);
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

  protected accessSummary(log: ShareUsageLog): string {
    return log.accessIp ? 'Access location recorded' : 'Access location not provided';
  }

  protected visitorLabel(log: ShareUsageLog): string {
    return log.accessedByName || log.accessedByUserId || 'Guest visitor';
  }
}
