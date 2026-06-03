import { HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiEndpoints } from '@app/core/api/api-endpoints';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  PlanFeaturePolicy,
  WorkspaceContext,
  WorkspaceSubscription,
  WorkspaceSummary,
  WorkspaceUsageSummary,
} from './workspace.models';

interface BackendWorkspaceContext {
  readonly workspaceId: string;
  readonly workspaceName: string;
  readonly role: WorkspaceSummary['currentUserRole'];
  readonly permissions: WorkspaceSummary['currentUserPermissions'];
  readonly activePricingPlan?: {
    readonly id: string;
    readonly name: string;
    readonly code: string;
  } | null;
  readonly activeSubscription?: {
    readonly id: string;
    readonly pricingPlanId: string;
    readonly status: string;
    readonly expiresAt?: string | null;
    readonly trialEndsAt?: string | null;
  } | null;
  readonly planFeaturePolicy?: BackendPlanFeaturePolicy | null;
  readonly generatedVersionLimit?: number | null;
  readonly storageLimitGb?: number | string | null;
  readonly approvalWorkflowAvailable?: boolean | null;
  readonly publicShareAvailability?: boolean | null;
  readonly teamMemberLimit?: number | null;
  readonly creditLimit?: number | string | null;
}

interface BackendPlanFeaturePolicy {
  readonly maxGeneratedVersionsPerRequest?: number | null;
  readonly maxBrands?: number | null;
  readonly maxProductServices?: number | null;
  readonly maxProjects?: number | null;
  readonly maxAssets?: number | null;
  readonly maxCreativeRequests?: number | null;
  readonly maxTeamMembers?: number | null;
  readonly maxStorageGb?: number | string | null;
  readonly monthlyCreditLimit?: number | string | null;
  readonly promptEnhancementEnabled?: boolean;
  readonly creativeGenerationEnabled?: boolean;
  readonly allowApprovalWorkflow?: boolean;
  readonly downloadEnabled?: boolean;
  readonly shareEnabled?: boolean;
  readonly allowPublicShareLinks?: boolean;
  readonly assetUploadEnabled?: boolean;
  readonly premiumQualityEnabled?: boolean;
  readonly allowVideoGeneration?: boolean;
  readonly voiceoverGenerationEnabled?: boolean;
  readonly allowAdvancedPromptIntelligence?: boolean;
  readonly allowTeamCollaboration?: boolean;
  readonly allowExportWithoutWatermark?: boolean;
}

interface CreditBalanceDto {
  readonly balance?: number | string | null;
  readonly reservedBalance?: number | string | null;
  readonly availableBalance?: number | string | null;
}

@Injectable({ providedIn: 'root' })
export class WorkspaceApiService {
  private readonly api = inject(ApiService);

  async getAccessibleWorkspaces(): Promise<readonly WorkspaceSummary[]> {
    const response = await firstValueFrom(this.api.get<WorkspaceSummary[]>(ApiEndpoints.workspaces.my));
    return unwrapApiResponse(response);
  }

  async getWorkspaceContext(workspaceId: string): Promise<WorkspaceContext> {
    const context = new HttpContext().set(SKIP_ERROR_TOAST, true);
    const [workspaceContextResponse, creditResponse] = await Promise.all([
      firstValueFrom(this.api.get<BackendWorkspaceContext>(ApiEndpoints.workspaces.context(workspaceId), { context })),
      firstValueFrom(this.api.get<CreditBalanceDto>(ApiEndpoints.usage.credits(workspaceId), { context })).catch(() => null),
    ]);

    return mapWorkspaceContext(
      unwrapApiResponse(workspaceContextResponse),
      creditResponse ? unwrapApiResponse(creditResponse) : null,
    );
  }
}

function mapWorkspaceContext(
  source: BackendWorkspaceContext,
  credits: CreditBalanceDto | null,
): WorkspaceContext {
  const featurePolicy = mapFeaturePolicy(source.planFeaturePolicy, source);
  return {
    workspace: {
      id: source.workspaceId,
      name: source.workspaceName,
      slug: source.workspaceName.toLowerCase().replace(/\s+/g, '-'),
      logoUrl: null,
      status: 'ACTIVE',
      language: 'ENGLISH',
      timezone: 'UTC',
      ownerId: '',
      currentUserRole: source.role,
      currentUserPermissions: source.permissions,
      createdAt: '',
      updatedAt: '',
    },
    subscription: mapSubscription(source),
    featurePolicy,
    usage: mapUsage(source, credits),
    featureToggles: featurePolicy.features
      ? Object.fromEntries(Object.entries(featurePolicy.features).map(([key, value]) => [key, value.enabled]))
      : {},
  };
}

function mapSubscription(source: BackendWorkspaceContext): WorkspaceSubscription | null {
  const subscription = source.activeSubscription;
  const plan = source.activePricingPlan;

  if (!subscription && !plan) {
    return null;
  }

  return {
    id: subscription?.id ?? null,
    status: subscription?.status ?? (plan ? 'ACTIVE' : null),
    planId: subscription?.pricingPlanId ?? plan?.id ?? null,
    planName: plan?.name ?? plan?.code ?? 'Workspace package',
    billingCycle: null,
    currentPeriodEnd: subscription?.expiresAt ?? null,
    trialEndsAt: subscription?.trialEndsAt ?? null,
  };
}

function mapFeaturePolicy(
  policy: BackendPlanFeaturePolicy | null | undefined,
  context: BackendWorkspaceContext,
): PlanFeaturePolicy {
  const creditLimit = toNumber(policy?.monthlyCreditLimit ?? context.creditLimit);
  const storageGb = toNumber(policy?.maxStorageGb ?? context.storageLimitGb);
  return {
    generatedVersionLimit: policy?.maxGeneratedVersionsPerRequest ?? context.generatedVersionLimit ?? null,
    approvalAvailable: policy?.allowApprovalWorkflow ?? context.approvalWorkflowAvailable ?? null,
    shareAvailable: policy?.allowPublicShareLinks ?? context.publicShareAvailability ?? null,
    features: {
      creativeGeneration: { enabled: policy?.creativeGenerationEnabled !== false },
      assetUpload: { enabled: policy?.assetUploadEnabled !== false },
      premiumQuality: { enabled: policy?.premiumQualityEnabled === true },
      approvalWorkflow: { enabled: policy?.allowApprovalWorkflow ?? context.approvalWorkflowAvailable ?? false },
      publicShare: { enabled: policy?.allowPublicShareLinks ?? context.publicShareAvailability ?? false },
      videoGeneration: { enabled: policy?.allowVideoGeneration === true },
      teamCollaboration: { enabled: policy?.allowTeamCollaboration === true },
    },
    limits: {
      credits: { limit: creditLimit, unit: 'credits' },
      storage: { limit: storageGb, unit: 'GB' },
      'assets.storage': { limit: storageGb, unit: 'GB' },
      generatedVersions: {
        limit: policy?.maxGeneratedVersionsPerRequest ?? context.generatedVersionLimit ?? null,
        unit: 'versions',
      },
      teamMembers: {
        limit: policy?.maxTeamMembers ?? context.teamMemberLimit ?? null,
        unit: 'members',
      },
    },
  };
}

function mapUsage(source: BackendWorkspaceContext, credits: CreditBalanceDto | null): WorkspaceUsageSummary {
  const availableCredits = toNumber(credits?.availableBalance ?? credits?.balance);
  const creditLimit = toNumber(source.planFeaturePolicy?.monthlyCreditLimit ?? source.creditLimit);
  const storageGb = toNumber(source.planFeaturePolicy?.maxStorageGb ?? source.storageLimitGb);
  return {
    creditsRemaining: availableCredits,
    storageRemainingBytes: storageGb === null ? null : storageGb * 1024 * 1024 * 1024,
    limits: {
      credits: {
        used: creditLimit !== null && availableCredits !== null ? Math.max(0, creditLimit - availableCredits) : null,
        limit: creditLimit,
        remaining: availableCredits,
        unit: 'credits',
      },
      storage: {
        limit: storageGb === null ? null : storageGb * 1024 * 1024 * 1024,
        unit: 'bytes',
      },
    },
  };
}

function toNumber(value: number | string | null | undefined): number | null {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null;
  }

  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  return null;
}

