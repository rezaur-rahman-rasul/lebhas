import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  CreditPackage,
  CreditPackagePayload,
  CreditPurchasePayload,
  Invoice,
  PaymentFilters,
  PlanFeaturePolicyPayload,
  PaymentProvider,
  PaymentProviderConfiguration,
  PaymentProviderConfigurationPayload,
  PaymentProviderPayload,
  PaymentSessionResponse,
  PaymentTransaction,
  PricingPlanDetail,
  PricingPlanPayload,
  SubscriptionPurchasePayload,
  SubscriptionRenewPayload,
  SubscriptionUpgradePayload,
} from '../models/payment.models';

type FilterValue = string | number | boolean;
type FilterParams = Record<string, FilterValue>;

@Injectable({ providedIn: 'root' })
export class PaymentApiService {
  private readonly api = inject(ApiService);

  getPaymentProviders(): Promise<readonly PaymentProvider[]> {
    return this.get<readonly PaymentProvider[]>('/api/v1/master/payment-providers');
  }

  getPricingPlans(): Promise<readonly PricingPlanDetail[]> {
    return this.get<readonly PricingPlanDetail[]>('/api/v1/master/pricing-plans');
  }

  createPricingPlan(payload: PricingPlanPayload): Promise<PricingPlanDetail> {
    return this.post<PricingPlanDetail, PricingPlanPayload>('/api/v1/master/pricing-plans', payload);
  }

  updatePricingPlan(pricingPlanId: string, payload: PricingPlanPayload): Promise<PricingPlanDetail> {
    return this.put<PricingPlanDetail, PricingPlanPayload>(
      `/api/v1/master/pricing-plans/${this.path(pricingPlanId)}`,
      payload,
    );
  }

  disablePricingPlan(pricingPlanId: string): Promise<PricingPlanDetail> {
    return this.delete<PricingPlanDetail>(`/api/v1/master/pricing-plans/${this.path(pricingPlanId)}`);
  }

  updatePlanFeaturePolicy(
    pricingPlanId: string,
    payload: PlanFeaturePolicyPayload,
  ): Promise<PricingPlanDetail> {
    return this.put<PricingPlanDetail, PlanFeaturePolicyPayload>(
      `/api/v1/master/pricing-plans/${this.path(pricingPlanId)}/feature-policy`,
      payload,
    );
  }

  createPaymentProvider(payload: PaymentProviderPayload): Promise<PaymentProvider> {
    return this.post<PaymentProvider, PaymentProviderPayload>(
      '/api/v1/master/payment-providers',
      payload,
    );
  }

  updatePaymentProvider(
    providerId: string,
    payload: PaymentProviderPayload,
  ): Promise<PaymentProvider> {
    return this.put<PaymentProvider, PaymentProviderPayload>(
      `/api/v1/master/payment-providers/${this.path(providerId)}`,
      payload,
    );
  }

  createProviderConfiguration(
    payload: PaymentProviderConfigurationPayload,
  ): Promise<PaymentProviderConfiguration> {
    return this.post<PaymentProviderConfiguration, PaymentProviderConfigurationPayload>(
      '/api/v1/master/payment-provider-configurations',
      payload,
    );
  }

  updateProviderConfiguration(
    configurationId: string,
    payload: PaymentProviderConfigurationPayload,
  ): Promise<PaymentProviderConfiguration> {
    return this.put<PaymentProviderConfiguration, PaymentProviderConfigurationPayload>(
      `/api/v1/master/payment-provider-configurations/${this.path(configurationId)}`,
      payload,
    );
  }

  getCreditPackages(): Promise<readonly CreditPackage[]> {
    return this.get<readonly CreditPackage[]>('/api/v1/master/credit-packages');
  }

  createCreditPackage(payload: CreditPackagePayload): Promise<CreditPackage> {
    return this.post<CreditPackage, CreditPackagePayload>('/api/v1/master/credit-packages', payload);
  }

  updateCreditPackage(
    creditPackageId: string,
    payload: CreditPackagePayload,
  ): Promise<CreditPackage> {
    return this.put<CreditPackage, CreditPackagePayload>(
      `/api/v1/master/credit-packages/${this.path(creditPackageId)}`,
      payload,
    );
  }

  purchaseSubscription(
    workspaceId: string,
    payload: SubscriptionPurchasePayload,
  ): Promise<PaymentSessionResponse> {
    return this.post<PaymentSessionResponse, SubscriptionPurchasePayload>(
      `/api/v1/workspaces/${this.path(workspaceId)}/subscriptions/purchase`,
      payload,
    );
  }

  upgradeSubscription(
    workspaceId: string,
    payload: SubscriptionUpgradePayload,
  ): Promise<PaymentSessionResponse> {
    return this.post<PaymentSessionResponse, SubscriptionUpgradePayload>(
      `/api/v1/workspaces/${this.path(workspaceId)}/subscriptions/upgrade`,
      payload,
    );
  }

  renewSubscription(
    workspaceId: string,
    payload: SubscriptionRenewPayload,
  ): Promise<PaymentSessionResponse> {
    return this.post<PaymentSessionResponse, SubscriptionRenewPayload>(
      `/api/v1/workspaces/${this.path(workspaceId)}/subscriptions/renew`,
      payload,
    );
  }

  purchaseCredits(
    workspaceId: string,
    payload: CreditPurchasePayload,
  ): Promise<PaymentSessionResponse> {
    return this.post<PaymentSessionResponse, CreditPurchasePayload>(
      `/api/v1/workspaces/${this.path(workspaceId)}/credits/purchase`,
      payload,
    );
  }

  getWorkspacePayments(
    workspaceId: string,
    filters?: PaymentFilters,
  ): Promise<readonly PaymentTransaction[]> {
    return this.get<readonly PaymentTransaction[]>(
      `/api/v1/workspaces/${this.path(workspaceId)}/payments`,
      filters,
    );
  }

  getWorkspacePaymentDetail(
    workspaceId: string,
    paymentTransactionId: string,
  ): Promise<PaymentTransaction> {
    return this.get<PaymentTransaction>(
      `/api/v1/workspaces/${this.path(workspaceId)}/payments/${this.path(paymentTransactionId)}`,
    );
  }

  getWorkspaceInvoices(workspaceId: string): Promise<readonly Invoice[]> {
    return this.get<readonly Invoice[]>(`/api/v1/workspaces/${this.path(workspaceId)}/invoices`);
  }

  private async get<T>(path: string, filters?: PaymentFilters): Promise<T> {
    const response = await firstValueFrom(this.api.get<T>(path, this.filters(filters)));
    return unwrapApiResponse(response);
  }

  private async post<T, TBody>(path: string, body: TBody): Promise<T> {
    const response = await firstValueFrom(this.api.post<T, TBody>(path, body));
    return unwrapApiResponse(response);
  }

  private async put<T, TBody>(path: string, body: TBody): Promise<T> {
    const response = await firstValueFrom(this.api.put<T, TBody>(path, body));
    return unwrapApiResponse(response);
  }

  private async delete<T>(path: string): Promise<T> {
    const response = await firstValueFrom(this.api.delete<T>(path));
    return unwrapApiResponse(response);
  }

  private filters(filters?: PaymentFilters): FilterParams {
    const params: FilterParams = {};

    for (const [key, value] of Object.entries(filters ?? {})) {
      if (value === null || value === undefined || value === '') {
        continue;
      }

      params[key] = value;
    }

    return params;
  }

  private path(value: string): string {
    return encodeURIComponent(value);
  }
}
