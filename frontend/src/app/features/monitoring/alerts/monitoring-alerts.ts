import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { MonitoringAlertCardComponent } from '../components/monitoring-alert-card/monitoring-alert-card';
import { MonitoringEmptyStateComponent } from '../components/monitoring-empty-state/monitoring-empty-state';
import { MonitoringFilterBarComponent } from '../components/monitoring-filter-bar/monitoring-filter-bar';
import { MonitoringLoadingStateComponent } from '../components/monitoring-loading-state/monitoring-loading-state';
import { MonitoringAlertType, MonitoringSeverity } from '../models/monitoring.models';
import { MonitoringStore } from '../state/monitoring.store';

@Component({
  selector: 'app-monitoring-alerts',
  standalone: true,
  imports: [
    RouterLink,
    ButtonComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    MonitoringAlertCardComponent,
    MonitoringEmptyStateComponent,
    MonitoringFilterBarComponent,
    MonitoringLoadingStateComponent,
  ],
  templateUrl: './monitoring-alerts.html',
  styleUrl: './monitoring-alerts.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonitoringAlertsPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(MonitoringStore);

  protected readonly accessDenied = computed(() => !this.permissions.canViewMonitoringAlerts());
  protected readonly hasAlerts = computed(() => this.store.alerts().length > 0);
  protected readonly sortedAlerts = computed(() => [
    ...this.store.alerts().filter((alert) => !alert.resolved && alert.severity === MonitoringSeverity.Critical),
    ...this.store.alerts().filter(
      (alert) => alert.resolved || alert.severity !== MonitoringSeverity.Critical,
    ),
  ]);

  constructor() {
    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      void this.store.loadMonitoringAlerts();
    });
  }

  protected refresh(): void {
    if (this.accessDenied()) {
      return;
    }

    void this.store.loadMonitoringAlerts();
  }

  protected updateAlertType(alertType: MonitoringAlertType | string | null): void {
    this.store.setMonitoringAlertType(alertType);
    this.refresh();
  }

  protected updateSeverity(severity: MonitoringSeverity | string | null): void {
    this.store.setMonitoringSeverity(severity);
    this.refresh();
  }

  protected updateUnresolvedOnly(value: boolean): void {
    this.store.setUnresolvedOnly(value);
    this.refresh();
  }

  protected clearFilters(): void {
    this.store.setMonitoringAlertType(null);
    this.store.setMonitoringSeverity(null);
    this.store.setUnresolvedOnly(true);
    this.refresh();
  }

  protected clearError(): void {
    this.store.clearError();
  }
}
