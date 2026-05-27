import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { HealthStatusCardComponent } from '../components/health-status-card/health-status-card';
import { MonitoringEmptyStateComponent } from '../components/monitoring-empty-state/monitoring-empty-state';
import { MonitoringLoadingStateComponent } from '../components/monitoring-loading-state/monitoring-loading-state';
import { MonitoringStore } from '../state/monitoring.store';

@Component({
  selector: 'app-system-health',
  standalone: true,
  imports: [
    RouterLink,
    ButtonComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    HealthStatusCardComponent,
    MonitoringEmptyStateComponent,
    MonitoringLoadingStateComponent,
  ],
  templateUrl: './system-health.html',
  styleUrl: './system-health.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SystemHealthPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(MonitoringStore);

  protected readonly accessDenied = computed(() => !this.permissions.canViewSystemHealth());
  protected readonly hasHealthData = computed(() => this.store.systemHealth().length > 0);

  constructor() {
    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      void this.store.loadSystemHealth();
    });
  }

  protected refresh(): void {
    if (this.accessDenied()) {
      return;
    }

    void this.store.loadSystemHealth();
  }

  protected clearError(): void {
    this.store.clearError();
  }
}
