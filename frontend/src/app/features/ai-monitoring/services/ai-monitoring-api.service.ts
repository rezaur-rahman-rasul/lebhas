import { HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiEndpoints } from '@app/core/api/api-endpoints';
import { ApiService } from '@app/core/api/api.service';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  AiFailureLog,
  AiLayerAnalytics,
  AiProviderHealth,
  AiProviderMetric,
  AiQualityScore,
  WorkspaceAiUsage,
  WorkspaceGenerationAnalytics,
} from '../models/ai-monitoring.models';

@Injectable({ providedIn: 'root' })
export class AiMonitoringApiService {
  private readonly api = inject(ApiService);
  private readonly quietContext = new HttpContext().set(SKIP_ERROR_TOAST, true);

  async getWorkspaceAiUsage(workspaceId: string): Promise<WorkspaceAiUsage> {
    return this.get<WorkspaceAiUsage>(ApiEndpoints.master.aiWorkspaceUsage(workspaceId));
  }

  async getWorkspaceQualityScores(workspaceId: string): Promise<readonly AiQualityScore[]> {
    void workspaceId;
    return [];
  }

  async getWorkspaceGenerationAnalytics(workspaceId: string): Promise<WorkspaceGenerationAnalytics> {
    const [usage, providerMetrics, failures] = await Promise.all([
      this.getWorkspaceAiUsage(workspaceId),
      this.getMasterProviderMetrics(),
      this.getMasterFailures(),
    ]);

    return {
      workspaceId,
      workspaceName: usage.workspaceName,
      usage,
      providerMetrics,
      layerAnalytics: [],
      qualityScores: [],
      failures: failures.filter((failure) => failure.creativeRequestId),
      updatedAt: usage.updatedAt,
    };
  }

  async getMasterProviderMetrics(): Promise<readonly AiProviderMetric[]> {
    return this.get<readonly AiProviderMetric[]>(ApiEndpoints.master.aiProviderMetrics);
  }

  async getMasterProviderHealth(): Promise<readonly AiProviderHealth[]> {
    return (await this.get<readonly Record<string, unknown>[]>(ApiEndpoints.master.aiProviderHealth)).map(mapProviderHealth);
  }

  async getMasterLayerAnalytics(): Promise<readonly AiLayerAnalytics[]> {
    return (await this.get<readonly Record<string, unknown>[]>(ApiEndpoints.master.aiLayerAnalytics)).map(mapLayerAnalytics);
  }

  async getMasterWorkspaceUsage(): Promise<readonly WorkspaceAiUsage[]> {
    return (await this.get<readonly Record<string, unknown>[]>(ApiEndpoints.master.aiCostUsage)).map(mapWorkspaceUsage);
  }

  async getMasterQualityScores(): Promise<readonly AiQualityScore[]> {
    return [];
  }

  async getMasterFailures(): Promise<readonly AiFailureLog[]> {
    return (await this.get<readonly Record<string, unknown>[]>(ApiEndpoints.master.aiFailures)).map(mapFailure);
  }

  private async get<T>(path: string): Promise<T> {
    const response = await firstValueFrom(this.api.get<T>(path, { context: this.quietContext }));
    return unwrapApiResponse(response);
  }

}

function mapProviderHealth(item: Record<string, unknown>): AiProviderHealth {
  const metadata = record(item['metadata']);
  const providerId = text(item['providerId']);
  return {
    id: text(item['id']) || providerId,
    providerId,
    providerName: text(metadata['providerName']) || text(metadata['providerCode']) || 'Provider not reported',
    modelName: text(metadata['modelName']) || '--',
    status: normalizeHealthStatus(text(item['status'])),
    failureReason: nullableText(item['failureReason']),
    recoveryStatus: booleanValue(item['circuitOpen']) ? 'Circuit open' : null,
    lastCheckedAt: text(item['lastCheckedAt']) || new Date(0).toISOString(),
    fallbackAvailable: !booleanValue(item['circuitOpen']),
    recommendedAction: booleanValue(item['circuitOpen']) ? 'Review provider routing and credential status.' : null,
  };
}

function mapLayerAnalytics(item: Record<string, unknown>): AiLayerAnalytics {
  const layerId = text(item['layerId']);
  const providerId = text(item['providerId']);
  return {
    id: text(item['id']) || `${layerId}-${providerId}`,
    layerId,
    layerName: text(item['layerName']) || 'Layer not reported',
    layerType: text(item['layerType']) || 'AI_LAYER',
    providerId,
    providerName: text(item['providerName']) || 'Provider not reported',
    modelName: text(item['modelName']) || '--',
    totalExecutions: numberValue(item['totalExecutions']),
    successfulExecutions: numberValue(item['successfulExecutions']),
    failedExecutions: numberValue(item['failedExecutions']),
    avgExecutionTimeMs: numberValue(item['avgExecutionTimeMs']),
    avgExecutionCostUsd: numberValue(item['avgExecutionCostUsd']),
    avgQualityScore: numberValue(item['avgQualityScore']),
    updatedAt: text(item['updatedAt']) || text(item['createdAt']) || new Date(0).toISOString(),
  };
}

function mapWorkspaceUsage(item: Record<string, unknown>): WorkspaceAiUsage {
  const workspaceId = text(item['workspaceId']);
  return {
    id: text(item['id']) || workspaceId,
    workspaceId,
    workspaceName: text(item['workspaceName']) || 'Workspace not reported',
    activePlan: text(item['activePlan']) || '--',
    totalGenerationRequests: numberValue(item['totalGenerationRequests']),
    totalGeneratedVersions: numberValue(item['totalGeneratedVersions']),
    totalCreditsConsumed: numberValue(item['totalCreditsConsumed']),
    totalEstimatedCostUsd: numberValue(item['totalEstimatedCostUsd']),
    totalFailures: numberValue(item['totalFailures']),
    avgGenerationTimeMs: numberValue(item['avgGenerationTimeMs']),
    highCostWarning: booleanValue(item['highCostWarning']),
    updatedAt: text(item['updatedAt']) || text(item['createdAt']) || new Date(0).toISOString(),
  };
}

function mapFailure(item: Record<string, unknown>): AiFailureLog {
  const layerId = text(item['layerId']);
  const providerId = text(item['providerId']);
  return {
    id: text(item['id']) || `${text(item['creativeRequestId'])}-${layerId}-${providerId}`,
    creativeRequestId: text(item['creativeRequestId']),
    layerId,
    layerName: text(item['layerName']) || 'Layer not reported',
    providerId,
    providerName: text(item['providerName']) || 'Provider not reported',
    modelName: text(item['modelName']) || '--',
    failureType: (text(item['failureType']) || 'UNKNOWN') as AiFailureLog['failureType'],
    failureReason: nullableText(item['failureReason']),
    retryAttempt: numberValue(item['retryAttempt']),
    fallbackTriggered: booleanValue(item['fallbackTriggered']),
    createdAt: text(item['createdAt']) || new Date(0).toISOString(),
  };
}

function normalizeHealthStatus(value: string): AiProviderHealth['status'] {
  const normalized = value.toUpperCase();
  if (normalized === 'DOWN' || normalized === 'FAILED') {
    return 'DOWN' as AiProviderHealth['status'];
  }
  if (normalized === 'DEGRADED' || normalized === 'WARNING') {
    return 'DEGRADED' as AiProviderHealth['status'];
  }
  if (normalized === 'COOLDOWN') {
    return 'COOLDOWN' as AiProviderHealth['status'];
  }
  return 'HEALTHY' as AiProviderHealth['status'];
}

function record(value: unknown): Record<string, unknown> {
  return typeof value === 'object' && value !== null ? (value as Record<string, unknown>) : {};
}

function text(value: unknown): string {
  return typeof value === 'string' ? value : value === null || value === undefined ? '' : String(value);
}

function nullableText(value: unknown): string | null {
  const result = text(value);
  return result || null;
}

function numberValue(value: unknown): number {
  const result = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(result) ? result : 0;
}

function booleanValue(value: unknown): boolean {
  return value === true || value === 'true';
}
