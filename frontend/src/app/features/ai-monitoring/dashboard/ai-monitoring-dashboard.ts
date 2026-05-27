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
import { AiCostSummaryCardComponent } from '../components/ai-cost-summary-card/ai-cost-summary-card';
import { AiMonitoringEmptyStateComponent } from '../components/ai-monitoring-empty-state/ai-monitoring-empty-state';
import { AiMonitoringLoadingStateComponent } from '../components/ai-monitoring-loading-state/ai-monitoring-loading-state';
import { FailureReasonCardComponent } from '../components/failure-reason-card/failure-reason-card';
import { RoutingModeBadgeComponent } from '../components/routing-mode-badge/routing-mode-badge';
import { AiProviderMetric } from '../models/ai-monitoring.models';
import { AiMonitoringStore } from '../state/ai-monitoring.store';

@Component({
  selector: 'app-ai-monitoring-dashboard',
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
    AiCostSummaryCardComponent,
    AiMonitoringEmptyStateComponent,
    AiMonitoringLoadingStateComponent,
    FailureReasonCardComponent,
    RoutingModeBadgeComponent,
  ],
  templateUrl: './ai-monitoring-dashboard.html',
  styleUrl: './ai-monitoring-dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiMonitoringDashboardPage implements OnInit {
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(AiMonitoringStore);

  protected readonly providerOptions = computed<readonly AiAnalyticsFilterOption[]>(() =>
    this.store.providerMetrics().map((provider) => ({
      id: provider.providerId,
      label: provider.providerName,
    })),
  );

  protected readonly layerOptions = computed<readonly AiAnalyticsFilterOption[]>(() =>
    this.store.layerAnalytics().map((layer) => ({
      id: layer.layerId,
      label: layer.layerName,
    })),
  );

  protected readonly hasData = computed(
    () =>
      this.store.providerMetrics().length > 0 ||
      this.store.providerHealth().length > 0 ||
      this.store.layerAnalytics().length > 0 ||
      this.store.workspaceAiUsage().length > 0 ||
      this.store.qualityScores().length > 0 ||
      this.store.aiFailures().length > 0,
  );

  protected readonly totalRequests = computed(() => {
    const workspaceTotal = this.store
      .workspaceAiUsage()
      .reduce((total, workspace) => total + workspace.totalGenerationRequests, 0);

    if (workspaceTotal > 0) {
      return workspaceTotal;
    }

    return this.store
      .providerMetrics()
      .reduce((total, provider) => total + provider.totalRequests, 0);
  });

  protected readonly averageGenerationTime = computed(() => {
    const workspaceTimes = this.store
      .workspaceAiUsage()
      .map((workspace) => workspace.avgGenerationTimeMs)
      .filter((value) => Number.isFinite(value));

    if (workspaceTimes.length) {
      return average(workspaceTimes);
    }

    const providerTimes = this.store
      .providerMetrics()
      .map((provider) => provider.avgLatencyMs)
      .filter((value) => Number.isFinite(value));

    return providerTimes.length ? average(providerTimes) : null;
  });

  protected readonly providerFailureRate = computed(() => {
    const totals = this.store.providerMetrics().reduce(
      (summary, provider) => ({
        requests: summary.requests + provider.totalRequests,
        failures: summary.failures + provider.failedRequests,
      }),
      { requests: 0, failures: 0 },
    );

    return totals.requests ? totals.failures / totals.requests : null;
  });

  protected readonly bestPerformingProvider = computed(() => this.store.bestQualityProvider());
  protected readonly recentFailuresPreview = computed(() => this.store.recentFailures().slice(0, 5));
  protected readonly qualityToCostProvider = computed(() =>
    maxBy(this.store.providerMetrics(), (provider) =>
      provider.avgCostUsd > 0 ? provider.avgQualityScore / provider.avgCostUsd : 0,
    ),
  );
  protected readonly recommendedOptimization = computed(() => null as string | null);

  ngOnInit(): void {
    if (this.permissions.canViewAiMonitoring()) {
      void this.store.loadMasterDashboardData();
    }
  }

  protected refresh(): void {
    if (this.permissions.canViewAiMonitoring()) {
      void this.store.loadMasterDashboardData();
    }
  }
}

function average(values: readonly number[]): number {
  return values.reduce((total, value) => total + value, 0) / values.length;
}

function maxBy<T>(items: readonly T[], selector: (item: T) => number): T | null {
  return [...items].sort((a, b) => selector(b) - selector(a))[0] ?? null;
}
