import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { HealthStatusCardComponent } from '../components/health-status-card/health-status-card';
import { MonitoringAlertCardComponent } from '../components/monitoring-alert-card/monitoring-alert-card';
import { MonitoringEmptyStateComponent } from '../components/monitoring-empty-state/monitoring-empty-state';
import { MonitoringLoadingStateComponent } from '../components/monitoring-loading-state/monitoring-loading-state';
import { MonitoringSummaryCardComponent } from '../components/monitoring-summary-card/monitoring-summary-card';
import { MonitoringStore } from '../state/monitoring.store';

@Component({
  selector: 'app-master-monitoring-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    ButtonComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    HealthStatusCardComponent,
    MonitoringAlertCardComponent,
    MonitoringEmptyStateComponent,
    MonitoringLoadingStateComponent,
    MonitoringSummaryCardComponent,
  ],
  templateUrl: './master-monitoring-dashboard.html',
  styleUrl: './master-monitoring-dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MasterMonitoringDashboardPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(MonitoringStore);

  protected readonly accessDenied = computed(() => !this.permissions.canViewMasterMonitoring());
  protected readonly hasHealthData = computed(() => this.store.systemHealth().length > 0);
  protected readonly hasAlerts = computed(() => this.store.alerts().length > 0);
  protected readonly recentCriticalAlerts = computed(() => this.store.criticalAlerts().slice(0, 3));

  constructor() {
    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      void this.store.loadMonitoringDashboard();
    });
  }

  protected refresh(): void {
    if (this.accessDenied()) {
      return;
    }

    void this.store.loadMonitoringDashboard();
  }

  protected clearError(): void {
    this.store.clearError();
  }

  protected summaryValue(summary: Record<string, unknown> | null, key: string): string | number {
    const value = summary?.[key];
    return typeof value === 'number' || typeof value === 'string' ? value : 'Unavailable';
  }
}
