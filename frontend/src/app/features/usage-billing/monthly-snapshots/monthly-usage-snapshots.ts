import { CurrencyPipe, DecimalPipe } from '@angular/common';
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
import { MonthlySnapshotCardComponent } from '../components/monthly-snapshot-card/monthly-snapshot-card';
import { UsageEmptyStateComponent } from '../components/usage-empty-state/usage-empty-state';
import { UsageFilterBarComponent } from '../components/usage-filter-bar/usage-filter-bar';
import { UsageLoadingStateComponent } from '../components/usage-loading-state/usage-loading-state';
import { CreditTransactionType, MonthlyUsageSnapshot, UsageType } from '../models/usage-billing.models';
import { UsageBillingStore } from '../state/usage-billing.store';

@Component({
  selector: 'app-monthly-usage-snapshots-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DecimalPipe,
    RouterLink,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    MonthlySnapshotCardComponent,
    UsageEmptyStateComponent,
    UsageFilterBarComponent,
    UsageLoadingStateComponent,
  ],
  templateUrl: './monthly-usage-snapshots.html',
  styleUrl: './monthly-usage-snapshots.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthlyUsageSnapshotsPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(UsageBillingStore);

  protected readonly activeWorkspaceId = this.workspace.activeWorkspaceId;
  protected readonly accessDenied = computed(() => !this.permissions.canViewUsageBilling());
  protected readonly snapshots = computed(() => {
    const selectedMonth = this.store.selectedMonth();
    const snapshots = this.store.monthlySnapshots();

    return selectedMonth
      ? snapshots.filter((snapshot) => snapshot.usageMonth.startsWith(selectedMonth))
      : snapshots;
  });
  protected readonly hasSnapshots = computed(() => this.snapshots().length > 0);

  constructor() {
    effect(() => {
      const workspaceId = this.activeWorkspaceId();
      if (!workspaceId || !this.permissions.canViewUsageBilling()) {
        return;
      }

      void this.store.loadMonthlySnapshots(workspaceId);
    });
  }

  protected refresh(): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId || !this.permissions.canViewUsageBilling()) {
      return;
    }

    void this.store.loadMonthlySnapshots(workspaceId);
  }

  protected updateMonth(month: string | null): void {
    this.store.setSelectedMonth(month);
  }

  protected updateUsageType(type: UsageType | string | null): void {
    this.store.setSelectedUsageType(type);
  }

  protected updateTransactionType(type: CreditTransactionType | string | null): void {
    this.store.setSelectedTransactionType(type);
  }

  protected clearFilters(): void {
    this.store.setSelectedMonth(null);
  }

  protected formatBytes(value: number): string {
    if (value <= 0) {
      return '0 B';
    }

    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const unitIndex = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1);
    const size = value / 1024 ** unitIndex;
    return `${size.toFixed(size >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
  }

  protected snapshotTrackBy(snapshot: MonthlyUsageSnapshot): string {
    return snapshot.id;
  }
}
