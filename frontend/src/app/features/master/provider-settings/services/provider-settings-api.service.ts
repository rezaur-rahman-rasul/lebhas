import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  AiProviderCredentialView,
  AiProviderView,
  CreateProviderCredentialRequest,
  CreateProviderRequest,
  OpenAiCostSyncResult,
  ProviderManagementCategory,
  ProviderModelsJsonView,
  SmsProviderActionResult,
  maskProviderSecret,
} from '../models/provider-credit-exchange.models';

@Injectable({ providedIn: 'root' })
export class ProviderSettingsApiService {
  private readonly api = inject(ApiService);
  private readonly providersPath = '/api/v1/master/providers';

  async getProviders(category?: ProviderManagementCategory | string | null): Promise<readonly AiProviderView[]> {
    const response = await firstValueFrom(this.api.get<readonly AiProviderView[]>(this.providersPath, category ? { category } : undefined));
    return safeArray(unwrapApiResponse(response)).map((item) => mapProvider(item));
  }

  async getProvider(providerId: string): Promise<AiProviderView> {
    const response = await firstValueFrom(this.api.get<AiProviderView>(`${this.providersPath}/${encodeURIComponent(providerId)}`));
    return mapProvider(unwrapApiResponse(response) as unknown as Record<string, unknown>);
  }

  async createProvider(payload: CreateProviderRequest): Promise<AiProviderView> {
    const response = await firstValueFrom(this.api.post<AiProviderView, unknown>(this.providersPath, toProviderPayload(payload)));
    const provider = mapProvider(unwrapApiResponse(response) as unknown as Record<string, unknown>);
    if (payload.apiKey?.trim()) {
      await this.saveCredential(provider.id, payload.apiKey, provider.active, payload.availableCreditBalance);
      return this.getProvider(provider.id);
    }
    return provider;
  }

  async updateProvider(providerId: string, payload: Partial<CreateProviderRequest>): Promise<AiProviderView> {
    const response = await firstValueFrom(
      this.api.put<AiProviderView, unknown>(`${this.providersPath}/${encodeURIComponent(providerId)}`, toProviderPayload(payload)),
    );
    const provider = mapProvider(unwrapApiResponse(response) as unknown as Record<string, unknown>);
    if (payload.apiKey?.trim() || (provider.credentialConfigured && Object.prototype.hasOwnProperty.call(payload, 'availableCreditBalance'))) {
      await this.saveCredential(providerId, payload.apiKey ?? '', provider.active, payload.availableCreditBalance);
      return this.getProvider(providerId);
    }
    return provider;
  }

  async toggleProvider(providerId: string, active: boolean): Promise<AiProviderView> {
    const response = await firstValueFrom(
      this.api.patch<AiProviderView, { readonly status: string }>(`${this.providersPath}/${encodeURIComponent(providerId)}/status`, {
        status: active ? 'ACTIVE' : 'INACTIVE',
      }),
    );
    return mapProvider(unwrapApiResponse(response) as unknown as Record<string, unknown>);
  }

  async deleteProvider(providerId: string): Promise<void> {
    await firstValueFrom(this.api.delete<unknown>(`${this.providersPath}/${encodeURIComponent(providerId)}`));
  }

  async testConnection(providerId: string): Promise<void> {
    await firstValueFrom(
      this.api.post<unknown, { readonly environment: string; readonly secret: null }>(
        `${this.providersPath}/${encodeURIComponent(providerId)}/test-connection`,
        { environment: 'SANDBOX', secret: null },
      ),
    );
  }

  async getModelsJson(providerId: string): Promise<ProviderModelsJsonView> {
    const response = await firstValueFrom(
      this.api.post<ProviderModelsJsonView, { readonly environment: string; readonly secret: null }>(
        `${this.providersPath}/${encodeURIComponent(providerId)}/models-json`,
        { environment: 'SANDBOX', secret: null },
      ),
    );
    return unwrapApiResponse(response) as ProviderModelsJsonView;
  }

  async testSms(providerId: string, mobileNumber: string, message?: string): Promise<SmsProviderActionResult> {
    const response = await firstValueFrom(
      this.api.post<SmsProviderActionResult, { readonly environment: string; readonly mobileNumber: string; readonly message: string | null }>(
        `${this.providersPath}/${encodeURIComponent(providerId)}/test-sms`,
        { environment: 'SANDBOX', mobileNumber, message: message?.trim() || null },
      ),
    );
    return unwrapApiResponse(response) as SmsProviderActionResult;
  }

  async checkSmsBalance(providerId: string): Promise<SmsProviderActionResult> {
    const response = await firstValueFrom(
      this.api.post<SmsProviderActionResult, { readonly environment: string; readonly secret: null }>(
        `${this.providersPath}/${encodeURIComponent(providerId)}/sms-balance`,
        { environment: 'SANDBOX', secret: null },
      ),
    );
    return unwrapApiResponse(response) as SmsProviderActionResult;
  }

  async checkProviderBalance(providerId: string): Promise<SmsProviderActionResult> {
    const response = await firstValueFrom(
      this.api.post<SmsProviderActionResult, { readonly environment: string; readonly secret: null }>(
        `${this.providersPath}/${encodeURIComponent(providerId)}/balance`,
        { environment: 'SANDBOX', secret: null },
      ),
    );
    return unwrapApiResponse(response) as SmsProviderActionResult;
  }

  async syncOpenAiCosts(providerId: string): Promise<OpenAiCostSyncResult> {
    const response = await firstValueFrom(
      this.api.post<OpenAiCostSyncResult, Record<string, never>>(
        `${this.providersPath}/${encodeURIComponent(providerId)}/sync-costs`,
        {},
      ),
    );
    return unwrapApiResponse(response) as OpenAiCostSyncResult;
  }

  async getCredentials(providerId: string): Promise<readonly AiProviderCredentialView[]> {
    const provider = await this.getProvider(providerId);
    if (!provider.credentialConfigured && !provider.maskedApiKey) {
      return [];
    }
    return [mapCredential({
      id: `${provider.id}:SANDBOX`,
      providerId: provider.id,
      credentialName: `${provider.providerName || provider.providerCode} credential`,
      maskedApiKey: provider.maskedApiKey,
      environment: 'SANDBOX',
      active: provider.active,
      createdAt: provider.createdAt,
      updatedAt: provider.updatedAt,
    })];
  }

  async createCredential(providerId: string, payload: CreateProviderCredentialRequest): Promise<AiProviderCredentialView> {
    const provider = await this.getProvider(providerId);
    await this.saveCredential(providerId, payload.apiKey, payload.active, normalizeOptionalNumber(provider.availableCreditBalance));
    return (await this.getCredentials(providerId))[0];
  }

  async updateCredential(providerId: string, credentialId: string, payload: CreateProviderCredentialRequest): Promise<AiProviderCredentialView> {
    const provider = await this.getProvider(providerId);
    await this.saveCredential(providerId, payload.apiKey, payload.active, normalizeOptionalNumber(provider.availableCreditBalance));
    return (await this.getCredentials(providerId))[0];
  }

  async deleteCredential(providerId: string, credentialId: string): Promise<void> {
    await firstValueFrom(this.api.delete<unknown>(`${this.providersPath}/${encodeURIComponent(providerId)}/credentials`, { environment: 'SANDBOX' }));
  }

  private async saveCredential(providerId: string, apiKey: string, active: boolean, availableCreditBalance: number | null | undefined): Promise<void> {
    await firstValueFrom(this.api.put<unknown, unknown>(`${this.providersPath}/${encodeURIComponent(providerId)}/credentials`, {
      environment: 'SANDBOX',
      secret: apiKey.trim() || null,
      webhookUrl: null,
      availableCreditBalance: normalizeOptionalNumber(availableCreditBalance),
      active,
    }));
  }
}

function normalizeOptionalNumber(value: unknown): number | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  const normalized = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(normalized) ? normalized : null;
}

function mapProvider(source: Record<string, unknown>): AiProviderView {
  const status = String(source['status'] ?? 'UNKNOWN');
  const providerType = String(source['providerType'] ?? 'AI');
  const active = Boolean(source['active'] ?? source['enabled'] ?? status === 'ACTIVE');
  const capabilities = Array.isArray(source['supportedCapabilities']) ? source['supportedCapabilities'].map(String) : [];
  return {
    id: String(source['id'] ?? ''),
    providerCode: String(source['providerCode'] ?? source['providerKey'] ?? ''),
    providerName: String(source['providerName'] ?? source['displayName'] ?? ''),
    displayName: String(source['displayName'] ?? source['providerName'] ?? ''),
    providerType,
    providerCategory: providerCategory(providerType, String(source['category'] ?? '')),
    category: String(source['category'] ?? ''),
    baseUrl: String(source['baseUrl'] ?? ''),
    defaultModel: String(source['defaultModel'] ?? ''),
    modelsEndpoint: String(source['modelsEndpoint'] ?? ''),
    modelsEndpointAuth: String(source['modelsEndpointAuth'] ?? 'BEARER'),
    apiKeyQueryParam: String(source['apiKeyQueryParam'] ?? ''),
    sendSmsEndpoint: String(source['sendSmsEndpoint'] ?? ''),
    balanceEndpoint: String(source['balanceEndpoint'] ?? ''),
    requestMethod: String(source['requestMethod'] ?? 'GET'),
    senderId: String(source['senderId'] ?? ''),
    otpLength: Number(source['otpLength'] ?? 6),
    otpExpiryMinutes: Number(source['otpExpiryMinutes'] ?? 5),
    resendCooldownSeconds: Number(source['resendCooldownSeconds'] ?? 60),
    maxAttempts: Number(source['maxAttempts'] ?? 3),
    balanceMonitoringEnabled: Boolean(source['balanceMonitoringEnabled']),
    healthCheckEnabled: Boolean(source['healthCheckEnabled'] ?? true),
    metadataJson: String(source['metadataJson'] ?? ''),
    status,
    healthStatus: String(source['lastTestStatus'] ?? source['healthStatus'] ?? 'UNKNOWN'),
    lastTestMessage: String(source['lastTestMessage'] ?? source['healthMessage'] ?? ''),
    priority: Number(source['priority'] ?? 100),
    rateLimitPerMinute: Number(source['rateLimitPerMinute'] ?? 60),
    costMultiplier: source['costMultiplier'] as number | string | null | undefined ?? 1,
    maskedApiKey: String(source['maskedApiKey'] ?? ''),
    maskedOpenAiAdminApiKey: String(source['maskedOpenAiAdminApiKey'] ?? ''),
    providerTopUpAmountUsd: source['providerTopUpAmountUsd'] as number | string | null | undefined ?? null,
    providerTopUpDate: source['providerTopUpDate'] ? String(source['providerTopUpDate']) : null,
    providerManualBalanceUsd: source['providerManualBalanceUsd'] as number | string | null | undefined ?? null,
    lastCostSyncAt: source['lastCostSyncAt'] ? String(source['lastCostSyncAt']) : null,
    totalCostSpentUsd: source['totalCostSpentUsd'] as number | string | null | undefined ?? null,
    estimatedRemainingBalanceUsd: source['estimatedRemainingBalanceUsd'] as number | string | null | undefined ?? null,
    estimatedInternalCredits: source['estimatedInternalCredits'] as number | string | null | undefined ?? null,
    costSyncEnabled: Boolean(source['costSyncEnabled']),
    balanceHealth: String(source['balanceHealth'] ?? 'UNKNOWN'),
    credentialConfigured: Boolean(source['credentialConfigured']),
    availableCreditBalance: source['availableCreditBalance'] as number | string | null | undefined ?? null,
    active,
    supportsImage: Boolean(source['supportsImage'] ?? capabilities.includes('IMAGE')),
    supportsText: Boolean(source['supportsText'] ?? (capabilities.includes('TEXT') || capabilities.length === 0)),
    supportsVideo: Boolean(source['supportsVideo'] ?? capabilities.includes('VIDEO')),
    supportsVoice: Boolean(source['supportsVoice'] ?? capabilities.includes('AUDIO')),
    supportsOtp: Boolean(source['supportsOtp'] ?? capabilities.includes('OTP')),
    supportsNotificationSms: Boolean(source['supportsNotificationSms'] ?? (capabilities.includes('NOTIFICATION_SMS') || capabilities.includes('NOTIFICATION'))),
    supportsMarketingSms: Boolean(source['supportsMarketingSms'] ?? (capabilities.includes('MARKETING_SMS') || capabilities.includes('MARKETING'))),
    createdAt: String(source['createdAt'] ?? ''),
    updatedAt: String(source['updatedAt'] ?? source['credentialUpdatedAt'] ?? ''),
  };
}

function mapCredential(source: Record<string, unknown>): AiProviderCredentialView {
  return {
    id: String(source['id'] ?? source['credentialId'] ?? ''),
    providerId: String(source['providerId'] ?? ''),
    credentialName: String(source['credentialName'] ?? source['name'] ?? 'Provider credential'),
    maskedApiKey: maskProviderSecret(String(source['maskedApiKey'] ?? source['maskedSecret'] ?? '')),
    environment: (source['environment'] ?? 'SANDBOX') as AiProviderCredentialView['environment'],
    active: Boolean(source['active']),
    createdAt: String(source['createdAt'] ?? ''),
    updatedAt: String(source['updatedAt'] ?? source['credentialUpdatedAt'] ?? ''),
  };
}

function toProviderPayload(payload: Partial<CreateProviderRequest>): Record<string, unknown> {
  const active = payload.active ?? true;
  const providerType = payload.providerType || 'AI';
  return {
    providerCode: payload.providerCode,
    displayName: payload.displayName || payload.providerName,
    providerType,
    description: payload.metadataJson || null,
    status: active ? 'ACTIVE' : 'INACTIVE',
    supportsSandbox: true,
    supportsLive: true,
    defaultEnvironment: 'SANDBOX',
    active,
    baseUrl: payload.baseUrl || null,
    defaultModel: payload.defaultModel || null,
    modelsEndpoint: payload.modelsEndpoint || null,
    modelsEndpointAuth: payload.modelsEndpointAuth || null,
    apiKeyQueryParam: payload.apiKeyQueryParam || null,
    sendSmsEndpoint: payload.sendSmsEndpoint || null,
    balanceEndpoint: payload.balanceEndpoint || null,
    requestMethod: payload.requestMethod || null,
    senderId: payload.senderId || null,
    otpLength: payload.otpLength ?? null,
    otpExpiryMinutes: payload.otpExpiryMinutes ?? null,
    resendCooldownSeconds: payload.resendCooldownSeconds ?? null,
    maxAttempts: payload.maxAttempts ?? null,
    balanceMonitoringEnabled: payload.balanceMonitoringEnabled ?? null,
    healthCheckEnabled: payload.healthCheckEnabled ?? null,
    supportedCapabilities: capabilities(payload),
    priority: payload.priority ?? 100,
    rateLimitPerMinute: payload.rateLimitPerMinute ?? 60,
    costMultiplier: payload.costMultiplier ?? 1,
    openAiAdminApiKey: payload.openAiAdminApiKey?.trim() || null,
    providerTopUpAmountUsd: normalizeOptionalNumber(payload.providerTopUpAmountUsd),
    providerTopUpDate: payload.providerTopUpDate || null,
    providerManualBalanceUsd: normalizeOptionalNumber(payload.providerManualBalanceUsd),
    costSyncEnabled: payload.costSyncEnabled ?? false,
    metadataJson: payload.metadataJson || null,
  };
}

function providerCategory(providerType: string, category: string): ProviderManagementCategory | string {
  const normalized = (providerType || category || 'AI').toUpperCase();
  if (normalized === 'NOTIFICATION' && category.toUpperCase().includes('EMAIL')) return 'EMAIL';
  if (normalized === 'NOTIFICATION' && category.toUpperCase().includes('SMS')) return 'SMS';
  if (['AI', 'SMS', 'EMAIL', 'STORAGE', 'PAYMENT'].includes(normalized)) {
    return normalized as ProviderManagementCategory;
  }
  return normalized;
}

function capabilities(payload: Partial<CreateProviderRequest>): readonly string[] {
  const providerType = String(payload.providerType ?? 'AI').toUpperCase();
  const items: string[] = [];
  if (providerType === 'SMS') {
    if (payload.supportsOtp) items.push('OTP');
    if (payload.supportsNotificationSms) items.push('NOTIFICATION_SMS');
    if (payload.supportsMarketingSms) items.push('MARKETING_SMS');
    return items;
  }
  if (providerType === 'AI') {
    if (payload.supportsText ?? true) items.push('TEXT');
    if (payload.supportsImage) items.push('IMAGE');
    if (payload.supportsVideo) items.push('VIDEO');
    if (payload.supportsVoice) items.push('AUDIO');
  }
  return items;
}

function safeArray(value: unknown): readonly Record<string, unknown>[] {
  if (Array.isArray(value)) {
    return value as readonly Record<string, unknown>[];
  }
  if (value && typeof value === 'object') {
    const source = value as Record<string, unknown>;
    if (Array.isArray(source['data'])) return source['data'] as readonly Record<string, unknown>[];
    if (Array.isArray(source['content'])) return source['content'] as readonly Record<string, unknown>[];
  }
  return [];
}
