import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiEndpoints } from '@app/core/api/api-endpoints';
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

interface PagedResult<T> {
  readonly items?: readonly T[];
  readonly totalItems?: number;
  readonly message?: string | null;
  readonly hasData?: boolean | null;
}

@Injectable({ providedIn: 'root' })
export class UsageBillingApiService {
  private readonly api = inject(ApiService);

  getUsageSummary(workspaceId: string): Promise<WorkspaceUsageSummary> {
    return this.get<WorkspaceUsageSummary>(ApiEndpoints.usage.summary(workspaceId));
  }

  getCurrentMonthUsage(workspaceId: string): Promise<WorkspaceUsageSummary> {
    return this.get<WorkspaceUsageSummary>(ApiEndpoints.usage.currentMonth(workspaceId));
  }

  getCreditLedger(
    workspaceId: string,
    filters?: UsageBillingFilters,
  ): Promise<readonly CreditLedger[]> {
    return this.get<readonly CreditLedger[]>(
      ApiEndpoints.usage.creditLedger(workspaceId),
      filters,
    );
  }

  getUsageBillingLogs(
    workspaceId: string,
    filters?: UsageBillingFilters,
  ): Promise<readonly UsageBillingLog[]> {
    return this.get<readonly UsageBillingLog[]>(
      ApiEndpoints.usage.billingLogs(workspaceId),
      filters,
    );
  }

  getDownloadUsage(
    workspaceId: string,
    filters?: UsageBillingFilters,
  ): Promise<readonly DownloadUsageLog[]> {
    return this.get<readonly DownloadUsageLog[]>(
      ApiEndpoints.usage.downloadUsage(workspaceId),
      filters,
    );
  }

  getShareUsage(
    workspaceId: string,
    filters?: UsageBillingFilters,
  ): Promise<readonly ShareUsageLog[]> {
    return this.get<readonly ShareUsageLog[]>(
      ApiEndpoints.usage.shareUsage(workspaceId),
      filters,
    );
  }

  getMonthlyUsageSnapshots(workspaceId: string): Promise<readonly MonthlyUsageSnapshot[]> {
    return this.get<readonly MonthlyUsageSnapshot[]>(ApiEndpoints.usage.monthlySnapshots(workspaceId));
  }

  getMasterWorkspaceUsage(): Promise<readonly MasterWorkspaceUsage[]> {
    return this.getList<Record<string, unknown>>(ApiEndpoints.usage.masterWorkspaces).then((items) =>
      items.map(mapMasterWorkspaceUsage),
    );
  }

  getMasterWorkspaceUsageDetail(workspaceId: string): Promise<MasterWorkspaceUsage> {
    return this.get<MasterWorkspaceUsage>(ApiEndpoints.usage.masterWorkspace(workspaceId));
  }

  getMasterAiCosts(): Promise<readonly MasterAiCostUsage[]> {
    return this.getList<Record<string, unknown>>(ApiEndpoints.usage.masterAiCosts).then((items) =>
      items.map(mapMasterAiCostUsage),
    );
  }

  getMasterTopCostWorkspaces(): Promise<readonly TopCostWorkspace[]> {
    return this.getList<Record<string, unknown>>(ApiEndpoints.usage.masterTopCostWorkspaces).then((items) =>
      items.map(mapTopCostWorkspace),
    );
  }

  getMasterPlanUtilization(): Promise<readonly PlanUtilization[]> {
    return this.getList<Record<string, unknown>>(ApiEndpoints.usage.masterPlanUtilization).then((items) =>
      items.map(mapPlanUtilization),
    );
  }

  private async getList<T>(path: string, filters?: UsageBillingFilters): Promise<readonly T[]> {
    const data = await this.get<readonly T[] | PagedResult<T> | null>(path, filters);
    if (Array.isArray(data)) {
      return data;
    }
    const pagedData = data as PagedResult<T> | null;
    return pagedData?.items ?? [];
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
}

function text(value: unknown, fallback = ''): string {
  return typeof value === 'string' && value.length > 0 ? value : fallback;
}

function numberValue(value: unknown): number {
  return typeof value === 'number' ? value : Number(value ?? 0) || 0;
}

function nullableNumber(value: unknown): number | null {
  return value === null || value === undefined || value === '' ? null : numberValue(value);
}

function mapMasterWorkspaceUsage(item: Record<string, unknown>): MasterWorkspaceUsage {
  const summary = item['currentMonthUsage'] && typeof item['currentMonthUsage'] === 'object'
    ? item['currentMonthUsage'] as WorkspaceUsageSummary
    : item as unknown as WorkspaceUsageSummary;
  const workspaceId = text(item['workspaceId']);
  return {
    workspaceId,
    workspaceName: text(item['workspaceName'], workspaceId ? `Workspace ${workspaceId.slice(0, 8)}` : 'Workspace'),
    currentPlanName: text(item['currentPlanName'] ?? item['pricingPlanName'], '') || null,
    currentMonthUsage: summary?.workspaceId ? summary : null,
    latestSnapshot: null,
    updatedAt: text(item['updatedAt']),
  };
}

function mapMasterAiCostUsage(item: Record<string, unknown>): MasterAiCostUsage {
  return {
    usageMonth: text(item['usageMonth'], text(item['createdAt'], 'Current period')),
    totalAiCostUsd: numberValue(item['totalAiCostUsd'] ?? item['estimatedCostUsd']),
    totalLayerExecutions: numberValue(item['totalLayerExecutions']),
    totalGeneratedVersions: numberValue(item['totalGeneratedVersions']),
    totalGenerationFailures: numberValue(item['totalGenerationFailures']),
    updatedAt: text(item['updatedAt'] ?? item['createdAt'], new Date(0).toISOString()),
  };
}

function mapTopCostWorkspace(item: Record<string, unknown>): TopCostWorkspace {
  const workspaceId = text(item['workspaceId']);
  return {
    workspaceId,
    workspaceName: text(item['workspaceName'], workspaceId ? `Workspace ${workspaceId.slice(0, 8)}` : 'Workspace'),
    pricingPlanName: text(item['pricingPlanName'] ?? item['currentPlanName'], '') || null,
    usageMonth: text(item['usageMonth']),
    totalAiCostUsd: numberValue(item['totalAiCostUsd']),
    usedCredits: numberValue(item['usedCredits']),
    generatedVersions: numberValue(item['generatedVersions'] ?? item['totalGeneratedVersions']),
    highCostWarning: Boolean(item['highCostWarning']),
  };
}

function mapPlanUtilization(item: Record<string, unknown>): PlanUtilization {
  const pricingPlanId = text(item['pricingPlanId']);
  return {
    pricingPlanId: pricingPlanId || text(item['workspaceId']),
    pricingPlanName: text(item['pricingPlanName'], pricingPlanId ? `Plan ${pricingPlanId.slice(0, 8)}` : 'Plan from backend'),
    workspaceCount: numberValue(item['workspaceCount'] ?? 1),
    totalUsedCredits: numberValue(item['totalUsedCredits'] ?? item['usedCredits']),
    totalGeneratedVersions: numberValue(item['totalGeneratedVersions'] ?? item['generatedVersions']),
    totalAiCostUsd: numberValue(item['totalAiCostUsd']),
    averageCreditUtilizationPercent: nullableNumber(item['averageCreditUtilizationPercent'] ?? item['creditUtilizationPercent']),
    averageStorageUtilizationPercent: nullableNumber(item['averageStorageUtilizationPercent'] ?? item['storageUtilizationPercent']),
    updatedAt: text(item['updatedAt'] ?? item['usageMonth']),
  };
}
