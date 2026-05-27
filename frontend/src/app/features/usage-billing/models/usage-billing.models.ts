export enum CreditTransactionType {
  Reserve = 'RESERVE',
  Finalize = 'FINALIZE',
  Refund = 'REFUND',
  ManualAdjustment = 'MANUAL_ADJUSTMENT',
  SystemAdjustment = 'SYSTEM_ADJUSTMENT',
  Expiry = 'EXPIRY',
}

export enum UsageType {
  PromptEnhancement = 'PROMPT_ENHANCEMENT',
  CreativeGeneration = 'CREATIVE_GENERATION',
  LayerExecution = 'LAYER_EXECUTION',
  GeneratedVersion = 'GENERATED_VERSION',
  Download = 'DOWNLOAD',
  PublicShare = 'PUBLIC_SHARE',
  Storage = 'STORAGE',
  Export = 'EXPORT',
}

export enum DownloadType {
  Preview = 'PREVIEW',
  Final = 'FINAL',
  Watermarked = 'WATERMARKED',
  Original = 'ORIGINAL',
  Export = 'EXPORT',
}

export interface UsageLimitSnapshot {
  readonly used: number;
  readonly limit: number | null;
  readonly remaining: number | null;
  readonly label?: string | null;
}

export interface UsageBillingFilters {
  readonly month?: string | null;
  readonly usageType?: UsageType | string | null;
  readonly transactionType?: CreditTransactionType | string | null;
  readonly from?: string | null;
  readonly to?: string | null;
  readonly page?: number | null;
  readonly size?: number | null;
}

export interface CreditLedger {
  readonly id: string;
  readonly workspaceId: string;
  readonly creativeRequestId: string | null;
  readonly generatedVersionId: string | null;
  readonly generationJobId: string | null;
  readonly transactionType: CreditTransactionType | string;
  readonly creditsAmount: number;
  readonly balanceBeforeTransaction: number;
  readonly balanceAfterTransaction: number;
  readonly referenceType: string | null;
  readonly referenceId: string | null;
  readonly description: string | null;
  readonly createdBy: string | null;
  readonly createdByName?: string | null;
  readonly createdByProfileImageUrl?: string | null;
  readonly createdAt: string;
}

export interface WorkspaceUsageSummary {
  readonly id: string;
  readonly workspaceId: string;
  readonly usageMonth: string;
  readonly usedCredits: number;
  readonly reservedCredits: number;
  readonly refundedCredits: number;
  readonly totalCreativeRequests: number;
  readonly totalGeneratedVersions: number;
  readonly totalLayerExecutions: number;
  readonly totalAiCostUsd: number;
  readonly totalUploads: number;
  readonly totalStorageBytes: number;
  readonly totalDownloads: number;
  readonly totalPublicShares: number;
  readonly totalPromptEnhancements: number;
  readonly totalGenerationFailures: number;
  readonly totalApiCalls: number;
  readonly updatedAt: string;
  readonly creditLimit?: UsageLimitSnapshot | null;
  readonly storageLimit?: UsageLimitSnapshot | null;
  readonly generatedVersionLimit?: UsageLimitSnapshot | null;
}

export interface UsageBillingLog {
  readonly id: string;
  readonly workspaceId: string;
  readonly usageType: UsageType | string;
  readonly referenceType: string | null;
  readonly referenceId: string | null;
  readonly creditsCharged: number;
  readonly estimatedCostUsd: number;
  readonly pricingPlanId: string | null;
  readonly pricingPlanName: string | null;
  readonly planFeaturePolicyId: string | null;
  readonly actorName?: string | null;
  readonly actorProfileImageUrl?: string | null;
  readonly createdAt: string;
}

export interface DownloadUsageLog {
  readonly id: string;
  readonly workspaceId: string;
  readonly generatedVersionId: string | null;
  readonly assetId: string | null;
  readonly downloadedBy: string | null;
  readonly downloadedByName: string | null;
  readonly downloadedByProfileImageUrl?: string | null;
  readonly downloadType: DownloadType | string;
  readonly ipAddress: string | null;
  readonly userAgent: string | null;
  readonly createdAt: string;
}

export interface ShareUsageLog {
  readonly id: string;
  readonly workspaceId: string;
  readonly shareLinkId: string | null;
  readonly generatedVersionId: string | null;
  readonly accessedByUserId: string | null;
  readonly accessedByName: string | null;
  readonly accessedByProfileImageUrl?: string | null;
  readonly accessIp: string | null;
  readonly userAgent: string | null;
  readonly referrer: string | null;
  readonly createdAt: string;
}

export interface MonthlyUsageSnapshot {
  readonly id: string;
  readonly workspaceId: string;
  readonly usageMonth: string;
  readonly pricingPlanId: string | null;
  readonly pricingPlanName: string | null;
  readonly subscriptionId: string | null;
  readonly usedCredits: number;
  readonly generatedVersions: number;
  readonly creativeRequests: number;
  readonly aiCostUsd: number;
  readonly storageBytes: number;
  readonly downloads: number;
  readonly publicShares: number;
  readonly createdAt: string;
}

export interface MasterWorkspaceUsage {
  readonly workspaceId: string;
  readonly workspaceName: string;
  readonly currentPlanName: string | null;
  readonly currentMonthUsage: WorkspaceUsageSummary | null;
  readonly latestSnapshot: MonthlyUsageSnapshot | null;
  readonly updatedAt: string;
}

export interface MasterAiCostUsage {
  readonly usageMonth: string;
  readonly totalAiCostUsd: number;
  readonly totalLayerExecutions: number;
  readonly totalGeneratedVersions: number;
  readonly totalGenerationFailures: number;
  readonly updatedAt: string;
}

export interface TopCostWorkspace {
  readonly workspaceId: string;
  readonly workspaceName: string;
  readonly pricingPlanName: string | null;
  readonly usageMonth: string;
  readonly totalAiCostUsd: number;
  readonly usedCredits: number;
  readonly generatedVersions: number;
  readonly highCostWarning?: boolean | null;
}

export interface PlanUtilization {
  readonly pricingPlanId: string;
  readonly pricingPlanName: string;
  readonly workspaceCount: number;
  readonly totalUsedCredits: number;
  readonly totalGeneratedVersions: number;
  readonly totalAiCostUsd: number;
  readonly averageCreditUtilizationPercent: number | null;
  readonly averageStorageUtilizationPercent: number | null;
  readonly updatedAt: string;
}

export interface UsageBillingDashboardData {
  readonly usageSummary: WorkspaceUsageSummary;
  readonly currentMonthUsage: WorkspaceUsageSummary;
  readonly monthlySnapshots: readonly MonthlyUsageSnapshot[];
}

export interface UsageBillingActionResult {
  readonly ok: boolean;
  readonly message?: string;
}
