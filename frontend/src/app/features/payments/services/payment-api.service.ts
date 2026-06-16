import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiEndpoints } from '@app/core/api/api-endpoints';
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
    return this.get<readonly PaymentProvider[]>(ApiEndpoints.master.paymentProviders);
  }

  getPricingPlans(): Promise<readonly PricingPlanDetail[]> {
    return this.get<readonly PricingPlanDetail[]>(ApiEndpoints.master.pricingPlans);
  }

  createPricingPlan(payload: PricingPlanPayload): Promise<PricingPlanDetail> {
    return this.post<PricingPlanDetail, PricingPlanPayload>(ApiEndpoints.master.pricingPlans, payload);
  }

  updatePricingPlan(pricingPlanId: string, payload: PricingPlanPayload): Promise<PricingPlanDetail> {
    return this.put<PricingPlanDetail, PricingPlanPayload>(
      ApiEndpoints.master.pricingPlan(pricingPlanId),
      payload,
    );
  }

  disablePricingPlan(pricingPlanId: string): Promise<PricingPlanDetail> {
    return this.patch<PricingPlanDetail, Record<string, never>>(
      ApiEndpoints.master.pricingPlanDeactivate(pricingPlanId),
      {},
    );
  }

  updatePlanFeaturePolicy(
    pricingPlanId: string,
    payload: PlanFeaturePolicyPayload,
  ): Promise<PricingPlanDetail> {
    return this.put<PricingPlanDetail, PlanFeaturePolicyPayload>(
      ApiEndpoints.master.pricingPlanFeaturePolicy(pricingPlanId),
      payload,
    );
  }

  createPaymentProvider(payload: PaymentProviderPayload): Promise<PaymentProvider> {
    return this.post<PaymentProvider, PaymentProviderPayload>(
      ApiEndpoints.master.paymentProviders,
      payload,
    );
  }

  updatePaymentProvider(
    providerId: string,
    payload: PaymentProviderPayload,
  ): Promise<PaymentProvider> {
    return this.put<PaymentProvider, PaymentProviderPayload>(
      ApiEndpoints.master.paymentProvider(providerId),
      payload,
    );
  }

  createProviderConfiguration(
    payload: PaymentProviderConfigurationPayload,
  ): Promise<PaymentProviderConfiguration> {
    return this.post<PaymentProviderConfiguration, PaymentProviderConfigurationPayload>(
      ApiEndpoints.master.paymentProviderConfigurations,
      payload,
    );
  }

  updateProviderConfiguration(
    configurationId: string,
    payload: PaymentProviderConfigurationPayload,
  ): Promise<PaymentProviderConfiguration> {
    return this.put<PaymentProviderConfiguration, PaymentProviderConfigurationPayload>(
      ApiEndpoints.master.paymentProviderConfiguration(configurationId),
      payload,
    );
  }

  getCreditPackages(): Promise<readonly CreditPackage[]> {
    return this.get<readonly CreditPackage[]>(ApiEndpoints.master.creditPackages);
  }

  createCreditPackage(payload: CreditPackagePayload): Promise<CreditPackage> {
    return this.post<CreditPackage, CreditPackagePayload>(ApiEndpoints.master.creditPackages, payload);
  }

  updateCreditPackage(
    creditPackageId: string,
    payload: CreditPackagePayload,
  ): Promise<CreditPackage> {
    return this.put<CreditPackage, CreditPackagePayload>(
      ApiEndpoints.master.creditPackage(creditPackageId),
      payload,
    );
  }

  purchaseSubscription(
    workspaceId: string,
    payload: SubscriptionPurchasePayload,
  ): Promise<PaymentSessionResponse> {
    return this.postPaymentSession<SubscriptionPurchasePayload>(
      ApiEndpoints.billing.purchaseSubscription(workspaceId),
      payload,
    );
  }

  upgradeSubscription(
    workspaceId: string,
    payload: SubscriptionUpgradePayload,
  ): Promise<PaymentSessionResponse> {
    return this.postPaymentSession<SubscriptionUpgradePayload>(
      ApiEndpoints.billing.upgradeSubscription(workspaceId),
      payload,
    );
  }

  renewSubscription(
    workspaceId: string,
    payload: SubscriptionRenewPayload,
  ): Promise<PaymentSessionResponse> {
    return this.postPaymentSession<SubscriptionRenewPayload>(
      ApiEndpoints.billing.renewSubscription(workspaceId),
      payload,
    );
  }

  purchaseCredits(
    workspaceId: string,
    payload: CreditPurchasePayload,
  ): Promise<PaymentSessionResponse> {
    return this.postPaymentSession<CreditPurchasePayload>(
      ApiEndpoints.billing.purchaseCredits(workspaceId),
      payload,
    );
  }

  getWorkspacePaymentGateways(workspaceId: string): Promise<readonly PaymentProvider[]> {
    return this.get<readonly PaymentProvider[]>(ApiEndpoints.billing.paymentGateways(workspaceId));
  }

  getWorkspacePayments(
    workspaceId: string,
    filters?: PaymentFilters,
  ): Promise<readonly PaymentTransaction[]> {
    return this.get<readonly PaymentTransaction[]>(
      ApiEndpoints.billing.payments(workspaceId),
      filters,
    );
  }

  getWorkspacePaymentDetail(
    workspaceId: string,
    paymentTransactionId: string,
  ): Promise<PaymentTransaction> {
    return this.get<PaymentTransaction>(
      ApiEndpoints.billing.payment(workspaceId, paymentTransactionId),
    );
  }

  getWorkspaceInvoices(workspaceId: string): Promise<readonly Invoice[]> {
    return this.get<readonly Invoice[]>(ApiEndpoints.billing.invoices(workspaceId));
  }

  private async get<T>(path: string, filters?: PaymentFilters): Promise<T> {
    const response = await firstValueFrom(this.api.get<T>(path, this.filters(filters)));
    return unwrapApiResponse(response);
  }

  private async post<T, TBody>(path: string, body: TBody): Promise<T> {
    const response = await firstValueFrom(this.api.post<T, TBody>(path, body));
    return unwrapApiResponse(response);
  }

  private async postPaymentSession<TBody>(path: string, body: TBody): Promise<PaymentSessionResponse> {
    const response = await firstValueFrom(this.api.post<Record<string, unknown>, TBody>(path, body));
    return normalizePaymentSession(unwrapApiResponse(response));
  }

  private async put<T, TBody>(path: string, body: TBody): Promise<T> {
    const response = await firstValueFrom(this.api.put<T, TBody>(path, body));
    return unwrapApiResponse(response);
  }

  private async patch<T, TBody>(path: string, body: TBody): Promise<T> {
    const response = await firstValueFrom(this.api.patch<T, TBody>(path, body));
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

function normalizePaymentSession(value: Record<string, unknown>): PaymentSessionResponse {
  return {
    paymentTransactionId: String(value['paymentTransactionId'] ?? ''),
    paymentStatus: String(value['paymentStatus'] ?? value['status'] ?? 'PENDING'),
    providerName: nullableString(value['providerName'] ?? value['providerCode']),
    providerSessionId: nullableString(value['providerSessionId']),
    paymentRedirectUrl: nullableString(value['paymentRedirectUrl'] ?? value['redirectUrl']),
    expiresAt: nullableString(value['expiresAt']),
  };
}

function nullableString(value: unknown): string | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }

  return String(value);
}
