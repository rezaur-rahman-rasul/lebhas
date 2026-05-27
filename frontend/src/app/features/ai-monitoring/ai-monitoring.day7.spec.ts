import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { AiFailureType, ProviderHealthStatus } from './models/ai-monitoring.models';
import { AiMonitoringApiService } from './services/ai-monitoring-api.service';
import { AiMonitoringStore } from './state/ai-monitoring.store';

const featureRoot = join(process.cwd(), 'src/app/features/ai-monitoring');

function read(relativePath: string): string {
  return readFileSync(join(process.cwd(), relativePath), 'utf8');
}

function collectFiles(
  directory: string,
  predicate: (filePath: string) => boolean,
  files: string[] = [],
): string[] {
  for (const entry of readdirSync(directory)) {
    const fullPath = join(directory, entry);
    const stats = statSync(fullPath);

    if (stats.isDirectory()) {
      collectFiles(fullPath, predicate, files);
      continue;
    }

    if (predicate(fullPath)) {
      files.push(fullPath);
    }
  }

  return files;
}

function relativeFeaturePath(filePath: string): string {
  return relative(featureRoot, filePath).replaceAll('\\', '/');
}

const providerMetrics = [
  {
    id: 'metric-expensive',
    providerId: 'provider-expensive',
    providerName: 'Sample Expensive Provider',
    modelName: 'sample-image-model',
    totalRequests: 100,
    successfulRequests: 91,
    failedRequests: 9,
    avgLatencyMs: 900,
    avgCostUsd: 0.5,
    avgQualityScore: 88,
    uptimePercentage: 95,
    lastFailureAt: '2026-05-24T10:00:00Z',
    lastSuccessAt: '2026-05-25T10:00:00Z',
    updatedAt: '2026-05-25T10:00:00Z',
  },
  {
    id: 'metric-fast',
    providerId: 'provider-fast',
    providerName: 'Sample Fast Provider',
    modelName: 'sample-fast-model',
    totalRequests: 80,
    successfulRequests: 79,
    failedRequests: 1,
    avgLatencyMs: 120,
    avgCostUsd: 0.25,
    avgQualityScore: 82,
    uptimePercentage: 99.3,
    lastFailureAt: null,
    lastSuccessAt: '2026-05-25T10:00:00Z',
    updatedAt: '2026-05-25T10:00:00Z',
  },
  {
    id: 'metric-cheap',
    providerId: 'provider-cheap',
    providerName: 'Sample Value Provider',
    modelName: 'sample-value-model',
    totalRequests: 60,
    successfulRequests: 56,
    failedRequests: 4,
    avgLatencyMs: 420,
    avgCostUsd: 0.05,
    avgQualityScore: 76,
    uptimePercentage: 97,
    lastFailureAt: '2026-05-24T11:00:00Z',
    lastSuccessAt: '2026-05-25T11:00:00Z',
    updatedAt: '2026-05-25T11:00:00Z',
  },
];

const providerHealth = [
  {
    id: 'health-fast',
    providerId: 'provider-fast',
    providerName: 'Sample Fast Provider',
    modelName: 'sample-fast-model',
    status: ProviderHealthStatus.Healthy,
    failureReason: null,
    recoveryStatus: 'Ready',
    lastCheckedAt: '2026-05-25T12:00:00Z',
    fallbackAvailable: true,
    recommendedAction: null,
  },
  {
    id: 'health-down',
    providerId: 'provider-expensive',
    providerName: 'Sample Expensive Provider',
    modelName: 'sample-image-model',
    status: ProviderHealthStatus.Down,
    failureReason: 'Failure rate exceeded threshold',
    recoveryStatus: 'Recovering',
    lastCheckedAt: '2026-05-25T12:00:00Z',
    fallbackAvailable: false,
    recommendedAction: 'Review provider health before routing more requests.',
  },
];

const layerAnalytics = [
  {
    id: 'layer-1',
    layerId: 'layer-dynamic-1',
    layerName: 'Backend Dynamic Layer',
    layerType: 'analysis',
    providerId: 'provider-fast',
    providerName: 'Sample Fast Provider',
    modelName: 'sample-fast-model',
    totalExecutions: 50,
    successfulExecutions: 48,
    failedExecutions: 2,
    avgExecutionTimeMs: 220,
    avgExecutionCostUsd: 0.1,
    avgQualityScore: 84,
    updatedAt: '2026-05-25T12:00:00Z',
  },
];

const workspaceUsage = [
  {
    id: 'usage-1',
    workspaceId: 'workspace-admin',
    workspaceName: 'Sample Admin Workspace',
    activePlan: 'Backend Dynamic Package',
    totalGenerationRequests: 12,
    totalGeneratedVersions: 30,
    totalCreditsConsumed: 44,
    totalEstimatedCostUsd: 15.75,
    totalFailures: 2,
    avgGenerationTimeMs: 520,
    highCostWarning: true,
    updatedAt: '2026-05-25T12:00:00Z',
  },
];

const qualityScores = [
  {
    id: 'quality-1',
    generatedVersionId: 'generated-version-1',
    workspaceId: 'workspace-admin',
    qualityLabel: 'GOOD',
    overallScore: 86,
    textReadabilityScore: 81,
    productPreservationScore: 90,
    brandingScore: 84,
    banglaTypographyScore: 88,
    compositionScore: 82,
    qualityNotes: 'Backend quality note',
    createdAt: '2026-05-25T12:00:00Z',
  },
];

const failures = [
  {
    id: 'failure-1',
    creativeRequestId: 'request-1',
    layerId: 'layer-dynamic-1',
    layerName: 'Backend Dynamic Layer',
    providerId: 'provider-fast',
    providerName: 'Sample Fast Provider',
    modelName: 'sample-fast-model',
    failureType: AiFailureType.Timeout,
    failureReason: null,
    retryAttempt: 2,
    fallbackTriggered: true,
    createdAt: '2026-05-25T12:00:00Z',
  },
];

function configureStore() {
  const allow = signal(true).asReadonly();
  const api = {
    getMasterProviderMetrics: vi.fn().mockResolvedValue(providerMetrics),
    getMasterProviderHealth: vi.fn().mockResolvedValue(providerHealth),
    getMasterLayerAnalytics: vi.fn().mockResolvedValue(layerAnalytics),
    getMasterWorkspaceUsage: vi.fn().mockResolvedValue(workspaceUsage),
    getMasterQualityScores: vi.fn().mockResolvedValue(qualityScores),
    getMasterFailures: vi.fn().mockResolvedValue(failures),
    getWorkspaceAiUsage: vi.fn().mockResolvedValue(workspaceUsage[0]),
    getWorkspaceQualityScores: vi.fn().mockResolvedValue(qualityScores),
    getWorkspaceGenerationAnalytics: vi.fn().mockResolvedValue({
      workspaceId: 'workspace-admin',
      workspaceName: 'Sample Admin Workspace',
      usage: workspaceUsage[0],
      providerMetrics,
      layerAnalytics,
      qualityScores,
      failures,
      updatedAt: '2026-05-25T12:00:00Z',
    }),
  };

  TestBed.configureTestingModule({
    providers: [
      AiMonitoringStore,
      { provide: AiMonitoringApiService, useValue: api },
      {
        provide: PermissionStore,
        useValue: {
          canViewAiMonitoring: allow,
          canViewProviderMetrics: allow,
          canViewLayerAnalytics: allow,
          canViewWorkspaceAiUsage: allow,
          canViewQualityScores: allow,
          canViewAiFailures: allow,
        },
      },
      { provide: NotificationStateService, useValue: { error: vi.fn() } },
    ],
  });

  return { store: TestBed.inject(AiMonitoringStore), api };
}

describe('Day 7 AI monitoring store behavior', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('computes cheapest, fastest, and best quality providers from backend data', async () => {
    const { store } = configureStore();

    await store.loadMasterDashboardData();

    expect(store.cheapestProvider()?.providerName).toBe('Sample Value Provider');
    expect(store.fastestProvider()?.providerName).toBe('Sample Fast Provider');
    expect(store.bestQualityProvider()?.providerName).toBe('Sample Expensive Provider');
  });

  it('updates date range and provider filter state', () => {
    const { store } = configureStore();

    store.setDateRange({ from: '2026-05-01', to: '2026-05-25' });
    store.setSelectedProvider('provider-fast');

    expect(store.selectedDateRange()).toEqual({ from: '2026-05-01', to: '2026-05-25' });
    expect(store.selectedProvider()).toBe('provider-fast');
  });

  it('loads ADMIN workspace usage, quality scores, and failures through workspace APIs', async () => {
    const { store, api } = configureStore();

    await store.loadWorkspaceAiUsage('workspace-admin');
    await store.loadWorkspaceQualityScores('workspace-admin');
    await store.loadWorkspaceGenerationAnalytics('workspace-admin');

    expect(api.getWorkspaceAiUsage).toHaveBeenCalledWith('workspace-admin');
    expect(api.getWorkspaceQualityScores).toHaveBeenCalledWith('workspace-admin');
    expect(api.getWorkspaceGenerationAnalytics).toHaveBeenCalledWith('workspace-admin');
    expect(store.workspaceAiUsage()[0]?.workspaceName).toBe('Sample Admin Workspace');
    expect(store.qualityScores()[0]?.banglaTypographyScore).toBe(88);
    expect(store.aiFailures()[0]?.fallbackTriggered).toBe(true);
  });
});

describe('Day 7 AI monitoring UI coverage', () => {
  it('loads the AI monitoring dashboard for MASTER and hides it for unauthorized CREW', () => {
    const dashboard = read('src/app/features/ai-monitoring/dashboard/ai-monitoring-dashboard.html');
    const permissionStore = read('src/app/core/permissions/permission.store.ts');

    expect(dashboard).toContain('AI monitoring dashboard');
    expect(dashboard).toContain('permissions.canViewAiMonitoring()');
    expect(dashboard).toContain('AI monitoring access is restricted');
    expect(permissionStore).toContain("this.role() === 'MASTER'");
    expect(permissionStore).toContain('return this.has(permission');
  });

  it('renders provider metrics and provider health status badges', () => {
    const metrics = read('src/app/features/ai-monitoring/provider-metrics/provider-metrics.html');
    const health = read('src/app/features/ai-monitoring/provider-health/provider-health.html');
    const badge = read('src/app/features/ai-monitoring/components/provider-status-badge/provider-status-badge.html');

    expect(metrics).toContain('totalRequests');
    expect(metrics).toContain('successfulRequests');
    expect(metrics).toContain('failedRequests');
    expect(metrics).toContain('avgLatencyMs');
    expect(metrics).toContain('avgCostUsd');
    expect(metrics).toContain('avgQualityScore');
    expect(metrics).toContain('uptimePercentage');
    expect(metrics).toContain('lastSuccessAt');
    expect(metrics).toContain('lastFailureAt');
    expect(health).toContain('provider.status');
    expect(badge).toContain('Provider status:');
  });

  it('renders layer analytics dynamically from backend data', () => {
    const layerPage = read('src/app/features/ai-monitoring/layer-analytics/layer-analytics.html');

    expect(layerPage).toContain('layer.layerName');
    expect(layerPage).toContain('layer.layerType');
    expect(layerPage).toContain('layer.providerName');
    expect(layerPage).toContain('layer.modelName');
    expect(layerPage).toContain('totalExecutions');
    expect(layerPage).toContain('successfulExecutions');
    expect(layerPage).toContain('failedExecutions');
    expect(layerPage).toContain('avgExecutionTimeMs');
    expect(layerPage).toContain('avgExecutionCostUsd');
    expect(layerPage).toContain('avgQualityScore');
  });

  it('renders workspace usage, quality scores, Bangla typography score, and AI failures', () => {
    const usage = read('src/app/features/ai-monitoring/workspace-usage/workspace-ai-usage.html');
    const quality = read('src/app/features/ai-monitoring/quality-scores/quality-scores.html');
    const failure = read('src/app/features/ai-monitoring/failures/ai-failures.html');
    const failureCard = read(
      'src/app/features/ai-monitoring/components/failure-reason-card/failure-reason-card.ts',
    );

    expect(usage).toContain('usage.workspaceName');
    expect(usage).toContain('usage.activePlan');
    expect(usage).toContain('totalGenerationRequests');
    expect(usage).toContain('totalGeneratedVersions');
    expect(usage).toContain('totalCreditsConsumed');
    expect(usage).toContain('AI cost increased unusually for this workspace.');
    expect(quality).toContain('score.generatedVersionId');
    expect(quality).toContain('Bangla typography');
    expect(quality).toContain('score.banglaTypographyScore');
    expect(failure).toContain('creativeRequestId');
    expect(failureCard).toContain('Backup tool was used.');
    expect(failureCard).toContain('No backup tool was used.');
  });

  it('renders loading, empty, and error-with-retry states across Day 7 pages', () => {
    const pages = [
      'src/app/features/ai-monitoring/dashboard/ai-monitoring-dashboard.html',
      'src/app/features/ai-monitoring/provider-metrics/provider-metrics.html',
      'src/app/features/ai-monitoring/provider-health/provider-health.html',
      'src/app/features/ai-monitoring/layer-analytics/layer-analytics.html',
      'src/app/features/ai-monitoring/workspace-usage/workspace-ai-usage.html',
      'src/app/features/ai-monitoring/quality-scores/quality-scores.html',
      'src/app/features/ai-monitoring/failures/ai-failures.html',
    ];
    const combined = pages.map(read).join('\n');

    expect(combined).toContain('app-ai-monitoring-loading-state');
    expect(combined).toContain('app-ai-monitoring-empty-state');
    expect(combined).toContain('app-error-state');
    expect(combined).toContain('Try again');
  });

  it('keeps mobile layouts overflow-safe with responsive cards and desktop-only tables', () => {
    const combined = [
      read('src/app/features/ai-monitoring/provider-metrics/provider-metrics.html'),
      read('src/app/features/ai-monitoring/provider-health/provider-health.html'),
      read('src/app/features/ai-monitoring/layer-analytics/layer-analytics.html'),
      read('src/app/features/ai-monitoring/failures/ai-failures.html'),
      read('src/app/features/ai-monitoring/components/ai-analytics-filter-bar/ai-analytics-filter-bar.html'),
    ].join('\n');

    expect(combined).toContain('lg:hidden');
    expect(combined).toContain('hidden overflow-hidden rounded-lg border border-border bg-surface lg:block');
    expect(combined).toContain('grid gap-3 md:grid-cols-2');
    expect(combined).toContain('min-w-0');
    expect(combined).not.toContain('w-screen');
  });
});

describe('Day 7 static guardrails', () => {
  it('does not hardcode provider names in production AI monitoring files', () => {
    const offenders = collectFiles(featureRoot, (filePath) => !filePath.endsWith('.spec.ts'))
      .filter((filePath) => /openai|gemini|claude|\bgpt\b|flux/i.test(readFileSync(filePath, 'utf8')))
      .map(relativeFeaturePath);

    expect(offenders).toEqual([]);
  });

  it('does not use legacy structural directives in AI monitoring templates', () => {
    const offenders = collectFiles(featureRoot, (filePath) => filePath.endsWith('.html'))
      .filter((filePath) => /\*ngIf\b|\*ngFor\b/.test(readFileSync(filePath, 'utf8')))
      .map(relativeFeaturePath);

    expect(offenders).toEqual([]);
  });

  it('keeps the AI monitoring API service on standard ApiResponse unwrapping', () => {
    const apiService = read('src/app/features/ai-monitoring/services/ai-monitoring-api.service.ts');
    const coreApi = read('src/app/core/api/api.service.ts');

    expect(apiService).toContain('unwrapApiResponse');
    expect(apiService).toContain('this.api.get<T>(path)');
    expect(coreApi).toContain('ApiResponse<T>');
  });
});
