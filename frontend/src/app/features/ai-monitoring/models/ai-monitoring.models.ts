export enum ProviderHealthStatus {
  Healthy = 'HEALTHY',
  Degraded = 'DEGRADED',
  Down = 'DOWN',
  Cooldown = 'COOLDOWN',
}

export enum AiFailureType {
  Timeout = 'TIMEOUT',
  RateLimit = 'RATE_LIMIT',
  ProviderDown = 'PROVIDER_DOWN',
  InvalidResponse = 'INVALID_RESPONSE',
  QualityFailure = 'QUALITY_FAILURE',
  CostLimitExceeded = 'COST_LIMIT_EXCEEDED',
  Unknown = 'UNKNOWN',
}

export enum QualityScoreLabel {
  Excellent = 'EXCELLENT',
  Good = 'GOOD',
  NeedsImprovement = 'NEEDS_IMPROVEMENT',
  Failed = 'FAILED',
}

export interface AiProviderMetric {
  readonly id: string;
  readonly providerId: string;
  readonly providerName: string;
  readonly modelName: string;
  readonly totalRequests: number;
  readonly successfulRequests: number;
  readonly failedRequests: number;
  readonly avgLatencyMs: number;
  readonly avgCostUsd: number;
  readonly avgQualityScore: number;
  readonly uptimePercentage: number;
  readonly lastFailureAt: string | null;
  readonly lastSuccessAt: string | null;
  readonly updatedAt: string;
}

export interface AiProviderHealth {
  readonly id: string;
  readonly providerId: string;
  readonly providerName: string;
  readonly modelName: string;
  readonly status: ProviderHealthStatus;
  readonly failureReason: string | null;
  readonly recoveryStatus: string | null;
  readonly lastCheckedAt: string;
  readonly fallbackAvailable: boolean;
  readonly recommendedAction: string | null;
}

export interface AiLayerAnalytics {
  readonly id: string;
  readonly layerId: string;
  readonly layerName: string;
  readonly layerType: string;
  readonly providerId: string;
  readonly providerName: string;
  readonly modelName: string;
  readonly totalExecutions: number;
  readonly successfulExecutions: number;
  readonly failedExecutions: number;
  readonly avgExecutionTimeMs: number;
  readonly avgExecutionCostUsd: number;
  readonly avgQualityScore: number;
  readonly updatedAt: string;
}

export interface WorkspaceAiUsage {
  readonly id: string;
  readonly workspaceId: string;
  readonly workspaceName: string;
  readonly activePlan: string;
  readonly totalGenerationRequests: number;
  readonly totalGeneratedVersions: number;
  readonly totalCreditsConsumed: number;
  readonly totalEstimatedCostUsd: number;
  readonly totalFailures: number;
  readonly avgGenerationTimeMs: number;
  readonly highCostWarning: boolean;
  readonly updatedAt: string;
}

export interface AiQualityScore {
  readonly id: string;
  readonly generatedVersionId: string;
  readonly workspaceId: string;
  readonly qualityLabel?: QualityScoreLabel | string | null;
  readonly overallScore: number;
  readonly textReadabilityScore: number;
  readonly productPreservationScore: number;
  readonly brandingScore: number;
  readonly banglaTypographyScore: number;
  readonly compositionScore: number;
  readonly qualityNotes: string | null;
  readonly createdAt: string;
}

export interface AiFailureLog {
  readonly id: string;
  readonly creativeRequestId: string;
  readonly layerId: string;
  readonly layerName: string;
  readonly providerId: string;
  readonly providerName: string;
  readonly modelName: string;
  readonly failureType: AiFailureType;
  readonly failureReason: string | null;
  readonly retryAttempt: number;
  readonly fallbackTriggered: boolean;
  readonly createdAt: string;
}

export interface WorkspaceGenerationAnalytics {
  readonly workspaceId: string;
  readonly workspaceName: string;
  readonly usage: WorkspaceAiUsage;
  readonly providerMetrics: readonly AiProviderMetric[];
  readonly layerAnalytics: readonly AiLayerAnalytics[];
  readonly qualityScores: readonly AiQualityScore[];
  readonly failures: readonly AiFailureLog[];
  readonly updatedAt: string;
}

export interface AiMonitoringActionResult {
  readonly ok: boolean;
  readonly message?: string;
}

export interface AiMonitoringDateRange {
  readonly from: string | null;
  readonly to: string | null;
}
