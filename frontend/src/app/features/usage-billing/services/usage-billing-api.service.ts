import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  CreditLedger,
  DownloadUsageLog,
  MasterAiCostUsage,
  MasterWorkspaceUsage,
  MonthlyUsageSnapshot,
  PlanUtilization,
  ShareUsageLog,
  TopCostWorkspace,
  UsageBillingFilters,
  UsageBillingLog,
  WorkspaceUsageSummary,
} from '../models/usage-billing.models';

@Injectable({ providedIn: 'root' })
export class UsageBillingApiService {
  private readonly api = inject(ApiService);

  getUsageSummary(workspaceId: string): Promise<WorkspaceUsageSummary> {
    return this.get<WorkspaceUsageSummary>(
      `/api/v1/workspaces/${this.workspacePath(workspaceId)}/usage-summary`,
    );
  }

  getCurrentMonthUsage(workspaceId: string): Promise<WorkspaceUsageSummary> {
    return this.get<WorkspaceUsageSummary>(
      `/api/v1/workspaces/${this.workspacePath(workspaceId)}/usage-summary/current-month`,
    );
  }

  getCreditLedger(
    workspaceId: string,
    filters?: UsageBillingFilters,
  ): Promise<readonly CreditLedger[]> {
    return this.get<readonly CreditLedger[]>(
      `/api/v1/workspaces/${this.workspacePath(workspaceId)}/credit-ledger`,
      filters,
    );
  }

  getUsageBillingLogs(
    workspaceId: string,
    filters?: UsageBillingFilters,
  ): Promise<readonly UsageBillingLog[]> {
    return this.get<readonly UsageBillingLog[]>(
      `/api/v1/workspaces/${this.workspacePath(workspaceId)}/usage-billing-logs`,
      filters,
    );
  }

  getDownloadUsage(
    workspaceId: string,
    filters?: UsageBillingFilters,
  ): Promise<readonly DownloadUsageLog[]> {
    return this.get<readonly DownloadUsageLog[]>(
      `/api/v1/workspaces/${this.workspacePath(workspaceId)}/download-usage`,
      filters,
    );
  }

  getShareUsage(
    workspaceId: string,
    filters?: UsageBillingFilters,
  ): Promise<readonly ShareUsageLog[]> {
    return this.get<readonly ShareUsageLog[]>(
      `/api/v1/workspaces/${this.workspacePath(workspaceId)}/share-usage`,
      filters,
    );
  }

  getMonthlyUsageSnapshots(workspaceId: string): Promise<readonly MonthlyUsageSnapshot[]> {
    return this.get<readonly MonthlyUsageSnapshot[]>(
      `/api/v1/workspaces/${this.workspacePath(workspaceId)}/monthly-usage-snapshots`,
    );
  }

  getMasterWorkspaceUsage(): Promise<readonly MasterWorkspaceUsage[]> {
    return this.get<readonly MasterWorkspaceUsage[]>('/api/v1/master/usage/workspaces');
  }

  getMasterWorkspaceUsageDetail(workspaceId: string): Promise<MasterWorkspaceUsage> {
    return this.get<MasterWorkspaceUsage>(
      `/api/v1/master/usage/workspaces/${this.workspacePath(workspaceId)}`,
    );
  }

  getMasterAiCosts(): Promise<readonly MasterAiCostUsage[]> {
    return this.get<readonly MasterAiCostUsage[]>('/api/v1/master/usage/ai-costs');
  }

  getMasterTopCostWorkspaces(): Promise<readonly TopCostWorkspace[]> {
    return this.get<readonly TopCostWorkspace[]>('/api/v1/master/usage/top-cost-workspaces');
  }

  getMasterPlanUtilization(): Promise<readonly PlanUtilization[]> {
    return this.get<readonly PlanUtilization[]>('/api/v1/master/usage/plan-utilization');
  }

  private async get<T>(path: string, filters?: UsageBillingFilters): Promise<T> {
    const response = await firstValueFrom(this.api.get<T>(path, this.filters(filters)));
    return unwrapApiResponse(response);
  }

  private filters(filters?: UsageBillingFilters): Record<string, string | number> {
    const params: Record<string, string | number> = {};

    for (const [key, value] of Object.entries(filters ?? {})) {
      if (value === null || value === undefined || value === '') {
        continue;
      }

      params[key] = value;
    }

    return params;
  }

  private workspacePath(workspaceId: string): string {
    return encodeURIComponent(workspaceId);
  }
}
