import { CurrencyPipe, DecimalPipe, PercentPipe } from '@angular/common';
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
import { AiMonitoringEmptyStateComponent } from '../components/ai-monitoring-empty-state/ai-monitoring-empty-state';
import { AiMonitoringLoadingStateComponent } from '../components/ai-monitoring-loading-state/ai-monitoring-loading-state';
import { ProviderMetricCardComponent } from '../components/provider-metric-card/provider-metric-card';
import { ProviderStatusBadgeComponent } from '../components/provider-status-badge/provider-status-badge';
import { AiProviderMetric, ProviderHealthStatus } from '../models/ai-monitoring.models';
import { AiMonitoringStore } from '../state/ai-monitoring.store';

@Component({
  selector: 'app-provider-metrics-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DecimalPipe,
    PercentPipe,
    RouterLink,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    AiAnalyticsFilterBarComponent,
    AiMonitoringEmptyStateComponent,
    AiMonitoringLoadingStateComponent,
    ProviderMetricCardComponent,
    ProviderStatusBadgeComponent,
  ],
  templateUrl: './provider-metrics.html',
  styleUrl: './provider-metrics.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderMetricsPage implements OnInit {
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(AiMonitoringStore);

  protected readonly providerOptions = computed<readonly AiAnalyticsFilterOption[]>(() =>
    this.store.providerMetrics().map((provider) => ({
      id: provider.providerId,
      label: provider.providerName,
    })),
  );

  protected readonly filteredMetrics = computed(() => {
    const selectedProvider = this.store.selectedProvider();
    return selectedProvider
      ? this.store.providerMetrics().filter((provider) => provider.providerId === selectedProvider)
      : this.store.providerMetrics();
  });

  protected readonly totalRequests = computed(() =>
    this.filteredMetrics().reduce((total, provider) => total + provider.totalRequests, 0),
  );

  protected readonly successfulRequests = computed(() =>
    this.filteredMetrics().reduce((total, provider) => total + provider.successfulRequests, 0),
  );

  protected readonly failedRequests = computed(() =>
    this.filteredMetrics().reduce((total, provider) => total + provider.failedRequests, 0),
  );

  protected readonly averageLatency = computed(() =>
    average(this.filteredMetrics().map((provider) => provider.avgLatencyMs)),
  );

  protected readonly averageCost = computed(() =>
    average(this.filteredMetrics().map((provider) => provider.avgCostUsd)),
  );

  protected readonly averageQuality = computed(() =>
    average(this.filteredMetrics().map((provider) => provider.avgQualityScore)),
  );

  protected readonly failureRate = computed(() => {
    const requests = this.totalRequests();
    return requests ? this.failedRequests() / requests : null;
  });

  protected readonly criticalMetrics = computed(() =>
    this.filteredMetrics().filter((provider) => provider.failedRequests > 0),
  );

  ngOnInit(): void {
    if (this.permissions.canViewProviderMetrics()) {
      void Promise.all([this.store.loadProviderMetrics(), this.store.loadProviderHealth()]);
    }
  }

  protected refresh(): void {
    if (this.permissions.canViewProviderMetrics()) {
      void Promise.all([this.store.loadProviderMetrics(), this.store.loadProviderHealth()]);
    }
  }

  protected providerStatus(provider: AiProviderMetric): ProviderHealthStatus | null {
    return (
      this.store
        .providerHealth()
        .find((health) => health.providerId === provider.providerId && health.modelName === provider.modelName)
        ?.status ?? null
    );
  }

  protected providerActivityLabel(provider: AiProviderMetric): string {
    const status = this.providerStatus(provider);

    if (!status) {
      return 'Status not reported';
    }

    return status === ProviderHealthStatus.Down ? 'Inactive' : 'Active';
  }
}

function average(values: readonly number[]): number | null {
  const validValues = values.filter((value) => Number.isFinite(value));
  return validValues.length
    ? validValues.reduce((total, value) => total + value, 0) / validValues.length
    : null;
}
