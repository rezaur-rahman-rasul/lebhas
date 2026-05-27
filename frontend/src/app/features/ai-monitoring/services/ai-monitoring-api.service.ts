import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
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

  async getWorkspaceAiUsage(workspaceId: string): Promise<WorkspaceAiUsage> {
    return this.get<WorkspaceAiUsage>(`/api/v1/workspaces/${this.workspacePath(workspaceId)}/ai-usage`);
  }

  async getWorkspaceQualityScores(workspaceId: string): Promise<readonly AiQualityScore[]> {
    return this.get<readonly AiQualityScore[]>(
      `/api/v1/workspaces/${this.workspacePath(workspaceId)}/quality-scores`,
    );
  }

  async getWorkspaceGenerationAnalytics(workspaceId: string): Promise<WorkspaceGenerationAnalytics> {
    return this.get<WorkspaceGenerationAnalytics>(
      `/api/v1/workspaces/${this.workspacePath(workspaceId)}/generation-analytics`,
    );
  }

  async getMasterProviderMetrics(): Promise<readonly AiProviderMetric[]> {
    return this.get<readonly AiProviderMetric[]>('/api/v1/master/ai/providers/metrics');
  }

  async getMasterProviderHealth(): Promise<readonly AiProviderHealth[]> {
    return this.get<readonly AiProviderHealth[]>('/api/v1/master/ai/providers/health');
  }

  async getMasterLayerAnalytics(): Promise<readonly AiLayerAnalytics[]> {
    return this.get<readonly AiLayerAnalytics[]>('/api/v1/master/ai/layers/analytics');
  }

  async getMasterWorkspaceUsage(): Promise<readonly WorkspaceAiUsage[]> {
    return this.get<readonly WorkspaceAiUsage[]>('/api/v1/master/ai/workspaces/usage');
  }

  async getMasterQualityScores(): Promise<readonly AiQualityScore[]> {
    return this.get<readonly AiQualityScore[]>('/api/v1/master/ai/quality-scores');
  }

  async getMasterFailures(): Promise<readonly AiFailureLog[]> {
    return this.get<readonly AiFailureLog[]>('/api/v1/master/ai/failures');
  }

  private async get<T>(path: string): Promise<T> {
    const response = await firstValueFrom(this.api.get<T>(path));
    return unwrapApiResponse(response);
  }

  private workspacePath(workspaceId: string): string {
    return encodeURIComponent(workspaceId);
  }
}
