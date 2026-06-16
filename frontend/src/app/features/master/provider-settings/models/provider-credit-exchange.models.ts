export type ProviderEnvironment = 'SANDBOX' | 'LIVE';
export type ProviderCreditAdjustmentType = 'ADD' | 'REDUCE' | 'RECONCILE';
export type ProviderManagementCategory = 'AI' | 'SMS' | 'EMAIL' | 'STORAGE' | 'PAYMENT';

export interface AiProviderView {
  readonly id: string;
  readonly providerCode: string;
  readonly providerName: string;
  readonly displayName?: string | null;
  readonly providerType?: string | null;
  readonly providerCategory?: ProviderManagementCategory | string | null;
  readonly category?: string | null;
  readonly baseUrl?: string | null;
  readonly defaultModel?: string | null;
  readonly modelsEndpoint?: string | null;
  readonly modelsEndpointAuth?: string | null;
  readonly apiKeyQueryParam?: string | null;
  readonly sendSmsEndpoint?: string | null;
  readonly balanceEndpoint?: string | null;
  readonly requestMethod?: 'GET' | 'POST' | string | null;
  readonly senderId?: string | null;
  readonly otpLength?: number | null;
  readonly otpExpiryMinutes?: number | null;
  readonly resendCooldownSeconds?: number | null;
  readonly maxAttempts?: number | null;
  readonly balanceMonitoringEnabled?: boolean | null;
  readonly healthCheckEnabled?: boolean | null;
  readonly metadataJson?: string | null;
  readonly status?: string | null;
  readonly healthStatus?: string | null;
  readonly lastTestMessage?: string | null;
  readonly priority?: number | null;
  readonly rateLimitPerMinute?: number | null;
  readonly costMultiplier?: number | string | null;
  readonly maskedApiKey?: string | null;
  readonly maskedOpenAiAdminApiKey?: string | null;
  readonly providerTopUpAmountUsd?: number | string | null;
  readonly providerTopUpDate?: string | null;
  readonly providerManualBalanceUsd?: number | string | null;
  readonly lastCostSyncAt?: string | null;
  readonly totalCostSpentUsd?: number | string | null;
  readonly estimatedRemainingBalanceUsd?: number | string | null;
  readonly estimatedInternalCredits?: number | string | null;
  readonly costSyncEnabled?: boolean | null;
  readonly balanceHealth?: string | null;
  readonly credentialConfigured?: boolean;
  readonly availableCreditBalance?: number | string | null;
  readonly active: boolean;
  readonly supportsImage: boolean;
  readonly supportsText: boolean;
  readonly supportsVideo: boolean;
  readonly supportsVoice: boolean;
  readonly supportsOtp?: boolean;
  readonly supportsNotificationSms?: boolean;
  readonly supportsMarketingSms?: boolean;
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

export interface ProviderModelsJsonView {
  readonly providerKey: string;
  readonly providerCode: string;
  readonly displayName: string;
  readonly environment: ProviderEnvironment;
  readonly endpoint: string;
  readonly httpStatus: number;
  readonly retrievedAt: string;
  readonly modelsJson: Record<string, unknown>;
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
  readonly providerType?: ProviderManagementCategory | string | null;
  readonly displayName?: string | null;
  readonly baseUrl?: string | null;
  readonly apiKey?: string | null;
  readonly defaultModel?: string | null;
  readonly modelsEndpoint?: string | null;
  readonly modelsEndpointAuth?: string | null;
  readonly apiKeyQueryParam?: string | null;
  readonly sendSmsEndpoint?: string | null;
  readonly balanceEndpoint?: string | null;
  readonly requestMethod?: 'GET' | 'POST' | string | null;
  readonly senderId?: string | null;
  readonly otpLength?: number | null;
  readonly otpExpiryMinutes?: number | null;
  readonly resendCooldownSeconds?: number | null;
  readonly maxAttempts?: number | null;
  readonly balanceMonitoringEnabled?: boolean | null;
  readonly healthCheckEnabled?: boolean | null;
  readonly priority?: number | null;
  readonly rateLimitPerMinute?: number | null;
  readonly costMultiplier?: number | null;
  readonly availableCreditBalance?: number | null;
  readonly openAiAdminApiKey?: string | null;
  readonly providerTopUpAmountUsd?: number | null;
  readonly providerTopUpDate?: string | null;
  readonly providerManualBalanceUsd?: number | null;
  readonly costSyncEnabled?: boolean | null;
  readonly metadataJson?: string | null;
  readonly active: boolean;
  readonly supportsImage: boolean;
  readonly supportsText: boolean;
  readonly supportsVideo: boolean;
  readonly supportsVoice: boolean;
  readonly supportsOtp?: boolean;
  readonly supportsNotificationSms?: boolean;
  readonly supportsMarketingSms?: boolean;
}

export interface SmsProviderActionResult {
  readonly providerCode: string;
  readonly success: boolean;
  readonly action: string;
  readonly httpStatus?: number | null;
  readonly message?: string | null;
  readonly safeEndpoint?: string | null;
  readonly response?: Record<string, unknown> | null;
  readonly testedAt?: string | null;
}

export interface OpenAiCostSyncResult {
  readonly providerId: string;
  readonly providerCode: string;
  readonly success: boolean;
  readonly message: string;
  readonly httpStatus?: number | null;
  readonly previousSpendUsd?: number | string | null;
  readonly totalCostSpentUsd?: number | string | null;
  readonly previousBalanceUsd?: number | string | null;
  readonly estimatedRemainingBalanceUsd?: number | string | null;
  readonly internalCredits?: number | string | null;
  readonly syncedAt?: string | null;
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

export type FreeSignupCreditMode = 'FIXED_CREDITS' | 'FIXED_USD_VALUE' | 'PERCENTAGE_OF_PROVIDER_POOL';

export interface CreditValuePolicyView {
  readonly id?: string | null;
  readonly currency: string;
  readonly creditUsdValue: number;
  readonly averageProviderCostPerCreativeUsd: number;
  readonly providerCostMultiplier: number;
  readonly calculatedCreativeCostUsd: number;
  readonly calculatedCreativeCreditCost: number;
  readonly freeSignupCreditEnabled: boolean;
  readonly freeSignupMode: FreeSignupCreditMode;
  readonly freeSignupCredits: number;
  readonly freeSignupUsdValue: number;
  readonly freeSignupPercentage: number;
  readonly freeSignupUsdEquivalent: number;
  readonly oneTimePerWorkspace: boolean;
  readonly minimumWalletBalanceWarning: number;
  readonly active: boolean;
  readonly effectiveFrom?: string | null;
  readonly updatedAt?: string | null;
}

export type CreditValuePolicyPayload = Omit<
  CreditValuePolicyView,
  'id' | 'calculatedCreativeCostUsd' | 'calculatedCreativeCreditCost' | 'freeSignupUsdEquivalent' | 'updatedAt'
>;

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

export type WorkspaceCreditAdjustmentType = 'ADD' | 'DEDUCT';

export interface WorkspaceCreditAdjustmentRequest {
  readonly creditsAmount: number;
  readonly referenceType?: string | null;
  readonly referenceId?: string | null;
  readonly description?: string | null;
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
  readonly lowBalanceProviders: number | readonly ProviderCreditPoolView[];
  readonly providerPools: readonly ProviderCreditPoolView[];
  readonly workspaceCredits?: readonly (WorkspaceCreditAccountView & { readonly workspaceName?: string | null; readonly lastActivityAt?: string | null })[];
  readonly recentCreditLedger: readonly CreditLedgerItemView[];
}

export interface GenerationCreditPreviewView {
  readonly requestedVersionCount: number;
  readonly estimatedCreditCost: number | null;
  readonly availableCredits: number;
  readonly remainingCreditsAfterGeneration: number | null;
  readonly creditStatus?: 'READY' | 'MAY_BE_INSUFFICIENT' | 'UNAVAILABLE' | null;
  readonly message?: string | null;
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
