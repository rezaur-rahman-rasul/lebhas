import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import {
  AiAnalyticsFilterBarComponent,
  AiAnalyticsFilterOption,
} from '../components/ai-analytics-filter-bar/ai-analytics-filter-bar';
import { AiHealthCardComponent } from '../components/ai-health-card/ai-health-card';
import { AiMonitoringEmptyStateComponent } from '../components/ai-monitoring-empty-state/ai-monitoring-empty-state';
import { AiMonitoringLoadingStateComponent } from '../components/ai-monitoring-loading-state/ai-monitoring-loading-state';
import { ProviderStatusBadgeComponent } from '../components/provider-status-badge/provider-status-badge';
import { AiProviderHealth, ProviderHealthStatus } from '../models/ai-monitoring.models';
import { AiMonitoringStore } from '../state/ai-monitoring.store';

@Component({
  selector: 'app-provider-health-page',
  standalone: true,
  imports: [
    RouterLink,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    AiAnalyticsFilterBarComponent,
    AiHealthCardComponent,
    AiMonitoringEmptyStateComponent,
    AiMonitoringLoadingStateComponent,
    ProviderStatusBadgeComponent,
  ],
  templateUrl: './provider-health.html',
  styleUrl: './provider-health.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderHealthPage implements OnInit {
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(AiMonitoringStore);
  protected readonly providerHealthStatus = ProviderHealthStatus;

  protected readonly providerOptions = computed<readonly AiAnalyticsFilterOption[]>(() =>
    this.store.providerHealth().map((provider) => ({
      id: provider.providerId,
      label: provider.providerName,
    })),
  );

  protected readonly filteredHealth = computed(() => {
    const selectedProvider = this.store.selectedProvider();
    return selectedProvider
      ? this.store.providerHealth().filter((provider) => provider.providerId === selectedProvider)
      : this.store.providerHealth();
  });

  protected readonly activeProviders = computed(() =>
    this.filteredHealth().filter((provider) => provider.status === ProviderHealthStatus.Healthy),
  );

  protected readonly degradedProviders = computed(() =>
    this.filteredHealth().filter((provider) => provider.status === ProviderHealthStatus.Degraded),
  );

  protected readonly failedProviders = computed(() =>
    this.filteredHealth().filter((provider) => provider.status === ProviderHealthStatus.Down),
  );

  protected readonly cooldownProviders = computed(() =>
    this.filteredHealth().filter((provider) => provider.status === ProviderHealthStatus.Cooldown),
  );

  protected readonly criticalProviders = computed(() =>
    this.filteredHealth().filter(
      (provider) =>
        provider.status === ProviderHealthStatus.Down ||
        provider.status === ProviderHealthStatus.Degraded,
    ),
  );

  ngOnInit(): void {
    if (this.permissions.canViewProviderMetrics()) {
      void this.store.loadProviderHealth();
    }
  }

  protected refresh(): void {
    if (this.permissions.canViewProviderMetrics()) {
      void this.store.loadProviderHealth();
    }
  }

  protected friendlyReason(provider: AiProviderHealth): string {
    if (!provider.failureReason) {
      return 'No failure reason reported by backend monitoring.';
    }

    const normalized = provider.failureReason.toLowerCase();

    if (normalized.includes('latency') && normalized.includes('p95')) {
      return 'This provider is slower than usual.';
    }

    if (normalized.includes('failure rate') && normalized.includes('threshold')) {
      return 'This tool failed too many times recently.';
    }

    if (normalized.includes('cost anomaly')) {
      return 'AI cost increased unusually for this workspace.';
    }

    return provider.failureReason;
  }

  protected recoveryStatus(provider: AiProviderHealth): string {
    return provider.recoveryStatus || 'Recovery status has not been reported yet.';
  }
}
