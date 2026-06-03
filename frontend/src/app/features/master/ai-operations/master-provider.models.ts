export type MasterProviderType = 'AI' | 'PAYMENT' | 'STORAGE' | 'NOTIFICATION';
export type MasterProviderStatus = 'ACTIVE' | 'INACTIVE' | 'DISABLED' | 'DEPRECATED';
export type MasterProviderEnvironment = 'SANDBOX' | 'LIVE';
export type MasterCredentialStatus = 'NOT_CONFIGURED' | 'CONFIGURED' | 'EXPIRED' | 'INVALID' | 'REVOKED';
export type ProviderConnectionTestStatus =
  | 'SUCCESS'
  | 'FAILED'
  | 'NOT_IMPLEMENTED'
  | 'NOT_CONFIGURED'
  | 'NOT_TESTED'
  | 'HEALTHY'
  | 'DEGRADED';

export interface MasterProviderView {
  readonly id: string;
  readonly providerKey?: string | null;
  readonly providerCode: string;
  readonly displayName: string;
  readonly providerType: MasterProviderType;
  readonly status: MasterProviderStatus;
  readonly category?: string | null;
  readonly description?: string | null;
  readonly supportsSandbox: boolean;
  readonly supportsLive: boolean;
  readonly defaultEnvironment: MasterProviderEnvironment;
  readonly credentialStatus: MasterCredentialStatus;
  readonly credentialConfigured?: boolean;
  readonly activeEnvironment: MasterProviderEnvironment;
  readonly webhookConfigured?: boolean;
  readonly webhookUrl?: string | null;
  readonly lastTestStatus?: ProviderConnectionTestStatus | null;
  readonly lastTestedAt?: string | null;
  readonly lastTestMessage?: string | null;
  readonly active: boolean;
  readonly secretsHidden: boolean;
  readonly credentialUpdatedAt?: string | null;
  readonly createdAt?: string;
  readonly updatedAt?: string;
}

export interface CreateMasterProviderRequest {
  readonly providerCode: string;
  readonly displayName: string;
  readonly providerType: MasterProviderType;
  readonly description?: string | null;
  readonly supportsSandbox: boolean;
  readonly supportsLive: boolean;
  readonly defaultEnvironment: MasterProviderEnvironment;
  readonly active: boolean;
}

export interface SaveProviderCredentialRequest {
  readonly environment: MasterProviderEnvironment;
  readonly secret?: string | null;
  readonly webhookUrl?: string | null;
  readonly active: boolean;
}

export interface TestProviderConnectionRequest {
  readonly environment: MasterProviderEnvironment;
  readonly secret?: string | null;
}

export interface ProviderCredentialSavedView {
  readonly providerId: string;
  readonly providerKey?: string | null;
  readonly providerCode: string;
  readonly displayName?: string | null;
  readonly category?: string | null;
  readonly environment: MasterProviderEnvironment;
  readonly credentialStatus: MasterCredentialStatus;
  readonly active: boolean;
  readonly secretsHidden: boolean;
}

export interface ProviderConnectionTestResult {
  readonly providerKey?: string | null;
  readonly providerCode: string;
  readonly displayName?: string | null;
  readonly category?: string | null;
  readonly environment: MasterProviderEnvironment;
  readonly success?: boolean;
  readonly status?: string;
  readonly testStatus: ProviderConnectionTestStatus;
  readonly latencyMs?: number | null;
  readonly testedAt: string;
  readonly message: string;
}
