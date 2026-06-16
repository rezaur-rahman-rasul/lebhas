import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  CreditValuePolicyPayload,
  CreditValuePolicyView,
  ProviderCreditExchangePolicyView,
} from '../models/provider-credit-exchange.models';

@Injectable({ providedIn: 'root' })
export class ExchangePolicyApiService {
  private readonly api = inject(ApiService);

  async getExchangePolicy(providerId: string): Promise<ProviderCreditExchangePolicyView> {
    return this.get(`/api/v1/master/providers/${encodeURIComponent(providerId)}/exchange-policy`);
  }

  async createExchangePolicy(providerId: string, payload: ProviderCreditExchangePolicyView): Promise<ProviderCreditExchangePolicyView> {
    return this.post(`/api/v1/master/providers/${encodeURIComponent(providerId)}/exchange-policy`, payload);
  }

  async updateExchangePolicy(providerId: string, payload: ProviderCreditExchangePolicyView): Promise<ProviderCreditExchangePolicyView> {
    return this.put(`/api/v1/master/providers/${encodeURIComponent(providerId)}/exchange-policy`, payload);
  }

  async getCreditValuePolicy(): Promise<CreditValuePolicyView> {
    return unwrapApiResponse(await firstValueFrom(this.api.get<CreditValuePolicyView>('/api/v1/master/monetization/credit-value-policy')));
  }

  async updateCreditValuePolicy(payload: CreditValuePolicyPayload): Promise<CreditValuePolicyView> {
    return unwrapApiResponse(await firstValueFrom(this.api.put<CreditValuePolicyView, CreditValuePolicyPayload>('/api/v1/master/monetization/credit-value-policy', payload)));
  }

  async previewCreditValuePolicy(payload: CreditValuePolicyPayload): Promise<CreditValuePolicyView> {
    return unwrapApiResponse(await firstValueFrom(this.api.post<CreditValuePolicyView, CreditValuePolicyPayload>('/api/v1/master/monetization/credit-value-policy/preview', payload)));
  }

  private async get(path: string): Promise<ProviderCreditExchangePolicyView> {
    return unwrapApiResponse(await firstValueFrom(this.api.get<ProviderCreditExchangePolicyView>(path)));
  }

  private async post(path: string, body: ProviderCreditExchangePolicyView): Promise<ProviderCreditExchangePolicyView> {
    return unwrapApiResponse(await firstValueFrom(this.api.post<ProviderCreditExchangePolicyView, ProviderCreditExchangePolicyView>(path, body)));
  }

  private async put(path: string, body: ProviderCreditExchangePolicyView): Promise<ProviderCreditExchangePolicyView> {
    return unwrapApiResponse(await firstValueFrom(this.api.put<ProviderCreditExchangePolicyView, ProviderCreditExchangePolicyView>(path, body)));
  }
}
