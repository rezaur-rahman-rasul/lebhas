export type ProviderEnvironment = 'SANDBOX' | 'LIVE';
export type ProviderCreditAdjustmentType = 'ADD' | 'REDUCE' | 'RECONCILE';

export interface AiProviderView {
  readonly id: string;
  readonly providerCode: string;
  readonly providerName: string;
  readonly displayName?: string | null;
  readonly baseUrl?: string | null;
  readonly defaultModel?: string | null;
  readonly metadataJson?: string | null;
  readonly status?: string | null;
  readonly healthStatus?: string | null;
  readonly priority?: number | null;
  readonly rateLimitPerMinute?: number | null;
  readonly costMultiplier?: number | string | null;
  readonly maskedApiKey?: string | null;
  readonly credentialConfigured?: boolean;
  readonly active: boolean;
  readonly supportsImage: boolean;
  readonly supportsText: boolean;
  readonly supportsVideo: boolean;
  readonly supportsVoice: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface AiProviderCredentialView {
  readonly id: string;
  readonly providerId: string;
  readonly credentialName: string;
  readonly maskedApiKey: string;
  readonly environment: ProviderEnvironment;
  readonly active: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreateProviderCredentialRequest {
  readonly credentialName: string;
  readonly apiKey: string;
  readonly environment: ProviderEnvironment;
  readonly active: boolean;
}

export interface CreateProviderRequest {
  readonly providerCode: string;
  readonly providerName: string;
  readonly displayName?: string | null;
  readonly baseUrl?: string | null;
  readonly apiKey?: string | null;
  readonly defaultModel?: string | null;
  readonly priority?: number | null;
  readonly rateLimitPerMinute?: number | null;
  readonly costMultiplier?: number | null;
  readonly metadataJson?: string | null;
  readonly active: boolean;
  readonly supportsImage: boolean;
  readonly supportsText: boolean;
  readonly supportsVideo: boolean;
  readonly supportsVoice: boolean;
}

export interface ProviderCreditPoolView {
  readonly providerId: string;
  readonly providerName: string;
  readonly currency: string;
  readonly providerBalanceAmount: number;
  readonly internalCreditEquivalent: number;
  readonly reservedInternalCredits: number;
  readonly usedInternalCredits: number;
  readonly availableInternalCredits: number;
  readonly lowBalanceThreshold: number;
  readonly updatedAt: string;
}

export interface ProviderCreditAdjustmentRequest {
  readonly adjustmentType: ProviderCreditAdjustmentType;
  readonly amount: number;
  readonly reason: string;
  readonly note?: string | null;
}

export interface ProviderCreditExchangePolicyView {
  readonly providerId: string;
  readonly internalCreditPerProviderUnit: number;
  readonly freeSignupCreditPercentage: number;
  readonly freeSignupCreditEnabled: boolean;
  readonly maxFreeSignupCredits: number;
  readonly minProviderBalanceRequired: number;
  readonly fallbackFreeCredits: number;
  readonly active: boolean;
  readonly updatedAt: string;
}

export interface WorkspaceCreditAccountView {
  readonly workspaceId: string;
  readonly availableCredits: number;
  readonly reservedCredits: number;
  readonly usedCredits: number;
  readonly refundedCredits: number;
  readonly freeCreditsGranted: boolean;
  readonly freeCreditsGrantedAt: string | null;
  readonly updatedAt: string;
}

export interface CreditLedgerItemView {
  readonly id: string;
  readonly workspaceId?: string | null;
  readonly providerId?: string | null;
  readonly transactionType: string;
  readonly creditAmount: number;
  readonly balanceBefore: number;
  readonly balanceAfter: number;
  readonly referenceType: string | null;
  readonly referenceId: string | null;
  readonly description: string | null;
  readonly createdAt: string;
}

export interface MasterCreditOverviewView {
  readonly totalProviderEquivalentCredits: number;
  readonly totalWorkspaceAvailableCredits: number;
  readonly totalWorkspaceReservedCredits: number;
  readonly totalWorkspaceUsedCredits: number;
  readonly totalFreeCreditsGranted: number;
  readonly lowBalanceProviders: number;
  readonly providerPools: readonly ProviderCreditPoolView[];
  readonly workspaceCredits?: readonly (WorkspaceCreditAccountView & { readonly workspaceName?: string | null; readonly lastActivityAt?: string | null })[];
  readonly recentCreditLedger: readonly CreditLedgerItemView[];
}

export interface GenerationCreditPreviewView {
  readonly requestedVersionCount: number;
  readonly estimatedCreditCost: number;
  readonly availableCredits: number;
  readonly remainingCreditsAfterGeneration: number;
  readonly packageLimit?: number | null;
  readonly canReserveCredits: boolean;
  readonly canQueueGeneration: boolean;
  readonly blockingReasons: readonly string[];
}

export function maskProviderSecret(value: string | null | undefined): string {
  if (!value) {
    return 'Not saved';
  }

  if (value.includes('*')) {
    return value;
  }

  const visible = value.slice(-4);
  const prefix = value.includes('-') ? value.split('-').slice(0, 2).join('-') : 'key';
  return `${prefix}-************************${visible}`;
}
