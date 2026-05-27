export enum SystemHealthStatus {
  Healthy = 'HEALTHY',
  Degraded = 'DEGRADED',
  Down = 'DOWN',
  Unknown = 'UNKNOWN',
}

export enum MonitoringAlertType {
  HighFailureRate = 'HIGH_FAILURE_RATE',
  PaymentFailureSpike = 'PAYMENT_FAILURE_SPIKE',
  StorageLimitWarning = 'STORAGE_LIMIT_WARNING',
  CreditAbuseWarning = 'CREDIT_ABUSE_WARNING',
  AiProviderDown = 'AI_PROVIDER_DOWN',
  HighLatency = 'HIGH_LATENCY',
  GenerationSpike = 'GENERATION_SPIKE',
  RedisFailure = 'REDIS_FAILURE',
  KafkaFailure = 'KAFKA_FAILURE',
}

export enum MonitoringSeverity {
  Info = 'INFO',
  Warning = 'WARNING',
  Error = 'ERROR',
  Critical = 'CRITICAL',
}

export interface SystemHealthEvent {
  readonly id: string;
  readonly serviceName: string;
  readonly eventType: string;
  readonly status: SystemHealthStatus | string;
  readonly detailsJson: string | null;
  readonly createdAt: string;
}

export interface MonitoringAlert {
  readonly id: string;
  readonly alertType: MonitoringAlertType | string;
  readonly severity: MonitoringSeverity | string;
  readonly title: string;
  readonly description: string;
  readonly workspaceId: string | null;
  readonly workspaceName: string | null;
  readonly relatedProviderId: string | null;
  readonly relatedProviderName: string | null;
  readonly resolved: boolean;
  readonly resolvedAt: string | null;
  readonly createdAt: string;
}

export interface AiProviderMonitoringSummary {
  readonly totalProviders?: number;
  readonly healthyProviders?: number;
  readonly degradedProviders?: number;
  readonly failedProviders?: number;
  readonly [key: string]: unknown;
}

export interface PaymentMonitoringSummary {
  readonly totalPayments?: number;
  readonly failedPayments?: number;
  readonly pendingPayments?: number;
  readonly [key: string]: unknown;
}

export interface WorkspaceMonitoringSummary {
  readonly totalWorkspaces?: number;
  readonly activeWorkspaces?: number;
  readonly affectedWorkspaces?: number;
  readonly [key: string]: unknown;
}

export interface MonitoringAlertFilters {
  readonly alertType?: MonitoringAlertType | string | null;
  readonly severity?: MonitoringSeverity | string | null;
  readonly unresolvedOnly?: boolean | null;
  readonly workspaceId?: string | null;
  readonly from?: string | null;
  readonly to?: string | null;
  readonly page?: number | null;
  readonly size?: number | null;
}

export interface MonitoringActionResult {
  readonly ok: boolean;
  readonly message?: string;
}
