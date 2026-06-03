import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { PermissionStore } from '@app/core/permissions/permission.store';
import {
  UiResourceState,
  uiResourceErrorKind,
} from '@app/shared/models/ui-resource-state.model';
import {
  AiFailureLog,
  AiLayerAnalytics,
  AiMonitoringActionResult,
  AiMonitoringDateRange,
  AiProviderHealth,
  AiProviderMetric,
  AiQualityScore,
  ProviderHealthStatus,
  WorkspaceAiUsage,
} from '../models/ai-monitoring.models';
import { AiMonitoringApiService } from '../services/ai-monitoring-api.service';

@Injectable({ providedIn: 'root' })
export class AiMonitoringStore {
  private readonly api = inject(AiMonitoringApiService);
  private readonly permissions = inject(PermissionStore);

  private readonly providerMetricsSignal = signal<readonly AiProviderMetric[]>([]);
  private readonly providerHealthSignal = signal<readonly AiProviderHealth[]>([]);
  private readonly layerAnalyticsSignal = signal<readonly AiLayerAnalytics[]>([]);
  private readonly workspaceAiUsageSignal = signal<readonly WorkspaceAiUsage[]>([]);
  private readonly qualityScoresSignal = signal<readonly AiQualityScore[]>([]);
  private readonly aiFailuresSignal = signal<readonly AiFailureLog[]>([]);
  private readonly selectedDateRangeSignal = signal<AiMonitoringDateRange>({ from: null, to: null });
  private readonly selectedProviderSignal = signal<string | null>(null);
  private readonly selectedLayerSignal = signal<string | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly endpointUnavailableSignal = signal(false);
  private readonly stateSignal = signal<UiResourceState<unknown>>({ status: 'idle' });

  readonly providerMetrics = this.providerMetricsSignal.asReadonly();
  readonly providerHealth = this.providerHealthSignal.asReadonly();
  readonly layerAnalytics = this.layerAnalyticsSignal.asReadonly();
  readonly workspaceAiUsage = this.workspaceAiUsageSignal.asReadonly();
  readonly qualityScores = this.qualityScoresSignal.asReadonly();
  readonly aiFailures = this.aiFailuresSignal.asReadonly();
  readonly selectedDateRange = this.selectedDateRangeSignal.asReadonly();
  readonly selectedProvider = this.selectedProviderSignal.asReadonly();
  readonly selectedLayer = this.selectedLayerSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly endpointUnavailable = this.endpointUnavailableSignal.asReadonly();
  readonly state = this.stateSignal.asReadonly();

  readonly healthyProviders = computed(() =>
    this.providerHealthSignal().filter((provider) => provider.status === ProviderHealthStatus.Healthy),
  );
  readonly degradedProviders = computed(() =>
    this.providerHealthSignal().filter((provider) => provider.status === ProviderHealthStatus.Degraded),
  );
  readonly downProviders = computed(() =>
    this.providerHealthSignal().filter((provider) => provider.status === ProviderHealthStatus.Down),
  );
  readonly totalCost = computed(() => {
    const workspaceCost = sum(
      this.workspaceAiUsageSignal(),
      (workspace) => workspace.totalEstimatedCostUsd,
    );

    if (workspaceCost > 0) {
      return workspaceCost;
    }

    const layerCost = sum(
      this.layerAnalyticsSignal(),
      (layer) => layer.avgExecutionCostUsd * layer.totalExecutions,
    );

    if (layerCost > 0) {
      return layerCost;
    }

    return sum(
      this.providerMetricsSignal(),
      (provider) => provider.avgCostUsd * provider.totalRequests,
    );
  });
  readonly averageQualityScore = computed(() =>
    average([
      ...this.providerMetricsSignal().map((provider) => provider.avgQualityScore),
      ...this.layerAnalyticsSignal().map((layer) => layer.avgQualityScore),
      ...this.qualityScoresSignal().map((score) => score.overallScore),
    ]),
  );
  readonly highestFailureProvider = computed(() =>
    maxBy(this.providerMetricsSignal(), (provider) => provider.failedRequests),
  );
  readonly cheapestProvider = computed(() =>
    minBy(this.providerMetricsSignal(), (provider) => provider.avgCostUsd),
  );
  readonly fastestProvider = computed(() =>
    minBy(this.providerMetricsSignal(), (provider) => provider.avgLatencyMs),
  );
  readonly bestQualityProvider = computed(() =>
    maxBy(this.providerMetricsSignal(), (provider) => provider.avgQualityScore),
  );
  readonly mostReliableProvider = computed(() =>
    maxBy(this.providerMetricsSignal(), (provider) => provider.uptimePercentage),
  );
  readonly highestCostWorkspace = computed(() =>
    maxBy(this.workspaceAiUsageSignal(), (workspace) => workspace.totalEstimatedCostUsd),
  );
  readonly recentFailures = computed(() =>
    [...this.aiFailuresSignal()].sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt)).slice(0, 10),
  );

  async loadMasterDashboardData(): Promise<AiMonitoringActionResult> {
    if (!this.permissions.canViewAiMonitoring()) {
      return this.restricted();
    }

    return this.run(async () => {
      const [providerMetrics, providerHealth, layerAnalytics, workspaceUsage, qualityScores, failures] =
        await Promise.all([
          this.api.getMasterProviderMetrics(),
          this.api.getMasterProviderHealth(),
          this.api.getMasterLayerAnalytics(),
          this.api.getMasterWorkspaceUsage(),
          this.api.getMasterQualityScores(),
          this.api.getMasterFailures(),
        ]);

      this.providerMetricsSignal.set(providerMetrics);
      this.providerHealthSignal.set(providerHealth);
      this.layerAnalyticsSignal.set(layerAnalytics);
      this.workspaceAiUsageSignal.set(workspaceUsage);
      this.qualityScoresSignal.set(qualityScores);
      this.aiFailuresSignal.set(failures);
    });
  }

  async loadProviderMetrics(): Promise<AiMonitoringActionResult> {
    if (!this.permissions.canViewProviderMetrics()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.providerMetricsSignal.set(await this.api.getMasterProviderMetrics());
    });
  }

  async loadProviderHealth(): Promise<AiMonitoringActionResult> {
    if (!this.permissions.canViewProviderMetrics()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.providerHealthSignal.set(await this.api.getMasterProviderHealth());
    });
  }

  async loadLayerAnalytics(): Promise<AiMonitoringActionResult> {
    if (!this.permissions.canViewLayerAnalytics()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.layerAnalyticsSignal.set(await this.api.getMasterLayerAnalytics());
    });
  }

  async loadMasterWorkspaceUsage(): Promise<AiMonitoringActionResult> {
    if (!this.permissions.canViewWorkspaceAiUsage()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.workspaceAiUsageSignal.set(await this.api.getMasterWorkspaceUsage());
    });
  }

  async loadMasterQualityScores(): Promise<AiMonitoringActionResult> {
    if (!this.permissions.canViewQualityScores()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.qualityScoresSignal.set(await this.api.getMasterQualityScores());
    });
  }

  async loadMasterFailures(): Promise<AiMonitoringActionResult> {
    if (!this.permissions.canViewAiFailures()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.aiFailuresSignal.set(await this.api.getMasterFailures());
    });
  }

  async loadWorkspaceAiUsage(workspaceId: string): Promise<AiMonitoringActionResult> {
    if (!this.permissions.canViewWorkspaceAiUsage()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.workspaceAiUsageSignal.set([await this.api.getWorkspaceAiUsage(workspaceId)]);
    });
  }

  async loadWorkspaceQualityScores(workspaceId: string): Promise<AiMonitoringActionResult> {
    if (!this.permissions.canViewQualityScores()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.qualityScoresSignal.set(await this.api.getWorkspaceQualityScores(workspaceId));
    });
  }

  async loadWorkspaceGenerationAnalytics(workspaceId: string): Promise<AiMonitoringActionResult> {
    if (!this.permissions.canViewWorkspaceAiUsage() && !this.permissions.canViewQualityScores()) {
      return this.restricted();
    }

    return this.run(async () => {
      const analytics = await this.api.getWorkspaceGenerationAnalytics(workspaceId);
      this.workspaceAiUsageSignal.set([analytics.usage]);
      this.providerMetricsSignal.set(analytics.providerMetrics);
      this.layerAnalyticsSignal.set(analytics.layerAnalytics);
      this.qualityScoresSignal.set(analytics.qualityScores);
      this.aiFailuresSignal.set(analytics.failures);
    });
  }

  setDateRange(dateRange: AiMonitoringDateRange): void {
    this.selectedDateRangeSignal.set(dateRange);
  }

  setSelectedProvider(providerId: string | null): void {
    this.selectedProviderSignal.set(providerId);
  }

  setSelectedLayer(layerId: string | null): void {
    this.selectedLayerSignal.set(layerId);
  }

  clearFilters(): void {
    this.selectedDateRangeSignal.set({ from: null, to: null });
    this.selectedProviderSignal.set(null);
    this.selectedLayerSignal.set(null);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  private async run(action: () => Promise<void>): Promise<AiMonitoringActionResult> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.endpointUnavailableSignal.set(false);
    this.stateSignal.set({ status: 'loading' });

    try {
      await action();
      this.stateSignal.set({ status: 'success', data: null });
      return { ok: true };
    } catch (error) {
      const normalized = normalizeHttpError(error);
      const message = this.friendlyError(error);
      this.endpointUnavailableSignal.set(normalized.status === 404);
      this.errorSignal.set(message);
      this.stateSignal.set({
        status: 'error',
        message,
        code: uiResourceErrorKind(normalized.status),
        retryable: normalized.status === 0 || normalized.status >= 500,
      });
      return { ok: false, message };
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private restricted(): AiMonitoringActionResult {
    const message = 'You do not have permission to view this AI monitoring data.';
    this.errorSignal.set(message);
    return { ok: false, message };
  }

  private friendlyError(error: unknown): string {
    const normalized = normalizeHttpError(error);
    if (normalized.status === 404) {
      return 'This monitoring API is not available yet. Empty monitoring panels will appear when the backend endpoint is connected.';
    }

    if (normalized.status === 204) {
      return '';
    }

    if (normalized.status === 403) {
      return 'You do not have permission to view this AI monitoring data.';
    }

    if (normalized.status >= 400 && normalized.status < 500) {
      return normalized.message || 'The monitoring request could not be validated.';
    }

    const text = `${normalized.message} ${normalized.errors
      .map((item) => `${item.code ?? ''} ${item.message}`)
      .join(' ')}`.toLowerCase();

    if (text.includes('latency') && text.includes('p95') && text.includes('degraded')) {
      return 'This provider is slower than usual.';
    }

    if (text.includes('failure rate') && text.includes('threshold')) {
      return 'This tool failed too many times recently.';
    }

    if (text.includes('cost anomaly')) {
      return 'AI cost increased unusually for this workspace.';
    }

    return 'We could not load AI monitoring data. Please try again.';
  }
}

function sum<T>(items: readonly T[], selector: (item: T) => number): number {
  return items.reduce((total, item) => total + selector(item), 0);
}

function average(values: readonly number[]): number | null {
  const validValues = values.filter((value) => Number.isFinite(value));
  return validValues.length ? sum(validValues, (value) => value) / validValues.length : null;
}

function maxBy<T>(items: readonly T[], selector: (item: T) => number): T | null {
  return [...items].sort((a, b) => selector(b) - selector(a))[0] ?? null;
}

function minBy<T>(items: readonly T[], selector: (item: T) => number): T | null {
  return [...items].sort((a, b) => selector(a) - selector(b))[0] ?? null;
}
