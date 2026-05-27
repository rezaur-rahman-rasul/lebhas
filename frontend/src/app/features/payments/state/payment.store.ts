import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  BillingCycle,
  CreditPackage,
  CreditPackagePayload,
  CreditPurchasePayload,
  CurrentSubscription,
  Invoice,
  PaymentActionResult,
  PaymentFilters,
  PaymentProvider,
  PaymentProviderConfiguration,
  PaymentProviderConfigurationPayload,
  PaymentProviderPayload,
  PaymentSessionResponse,
  PaymentStatus,
  PaymentTransaction,
  PricingPlanForPurchase,
  SelectedCreditPurchaseSummary,
  SelectedSubscriptionSummary,
  SubscriptionPurchasePayload,
  SubscriptionRenewPayload,
  SubscriptionUpgradePayload,
} from '../models/payment.models';
import { PaymentApiService } from '../services/payment-api.service';

@Injectable({ providedIn: 'root' })
export class PaymentStore {
  private readonly api = inject(PaymentApiService);
  private readonly permissions = inject(PermissionStore);
  private readonly notifications = inject(NotificationStateService);

  private readonly paymentProvidersSignal = signal<readonly PaymentProvider[]>([]);
  private readonly providerConfigurationsSignal = signal<readonly PaymentProviderConfiguration[]>([]);
  private readonly creditPackagesSignal = signal<readonly CreditPackage[]>([]);
  private readonly currentSubscriptionSignal = signal<CurrentSubscription | null>(null);
  private readonly pricingPlansForPurchaseSignal = signal<readonly PricingPlanForPurchase[]>([]);
  private readonly selectedPricingPlanSignal = signal<PricingPlanForPurchase | null>(null);
  private readonly selectedBillingCycleSignal = signal<BillingCycle | string | null>(null);
  private readonly selectedCreditPackageSignal = signal<CreditPackage | null>(null);
  private readonly paymentTransactionsSignal = signal<readonly PaymentTransaction[]>([]);
  private readonly invoicesSignal = signal<readonly Invoice[]>([]);
  private readonly activePaymentSessionSignal = signal<PaymentSessionResponse | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly paymentProviders = this.paymentProvidersSignal.asReadonly();
  readonly providerConfigurations = this.providerConfigurationsSignal.asReadonly();
  readonly creditPackages = this.creditPackagesSignal.asReadonly();
  readonly currentSubscription = this.currentSubscriptionSignal.asReadonly();
  readonly pricingPlansForPurchase = this.pricingPlansForPurchaseSignal.asReadonly();
  readonly selectedPricingPlan = this.selectedPricingPlanSignal.asReadonly();
  readonly selectedBillingCycle = this.selectedBillingCycleSignal.asReadonly();
  readonly selectedCreditPackage = this.selectedCreditPackageSignal.asReadonly();
  readonly paymentTransactions = this.paymentTransactionsSignal.asReadonly();
  readonly invoices = this.invoicesSignal.asReadonly();
  readonly activePaymentSession = this.activePaymentSessionSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly activeProviders = computed(() =>
    this.paymentProvidersSignal().filter((provider) => provider.isEnabled),
  );
  readonly disabledProviders = computed(() =>
    this.paymentProvidersSignal().filter((provider) => !provider.isEnabled),
  );
  readonly sandboxProviders = computed(() =>
    this.paymentProvidersSignal().filter((provider) => provider.sandboxEnabled),
  );
  readonly liveProviders = computed(() =>
    this.paymentProvidersSignal().filter((provider) => provider.liveEnabled),
  );
  readonly activeCreditPackages = computed(() =>
    [...this.creditPackagesSignal()]
      .filter((creditPackage) => creditPackage.isActive)
      .sort((a, b) => a.sortOrder - b.sortOrder),
  );
  readonly selectedSubscriptionSummary = computed<SelectedSubscriptionSummary | null>(() => {
    const plan = this.selectedPricingPlanSignal();
    if (!plan) {
      return null;
    }

    return {
      pricingPlanId: plan.id,
      packageName: plan.name,
      billingCycle: this.selectedBillingCycleSignal() ?? plan.billingCycle,
      amount: plan.price,
      currency: plan.currency,
    };
  });
  readonly selectedCreditPurchaseSummary = computed<SelectedCreditPurchaseSummary | null>(() => {
    const creditPackage = this.selectedCreditPackageSignal();
    if (!creditPackage) {
      return null;
    }

    return {
      creditPackageId: creditPackage.id,
      packageName: creditPackage.name,
      credits: creditPackage.credits,
      bonusCredits: creditPackage.bonusCredits,
      amount: creditPackage.price,
      currency: creditPackage.currency,
    };
  });
  readonly successfulPayments = computed(() =>
    this.paymentTransactionsSignal().filter((transaction) => transaction.status === PaymentStatus.Success),
  );
  readonly failedPayments = computed(() =>
    this.paymentTransactionsSignal().filter((transaction) => transaction.status === PaymentStatus.Failed),
  );
  readonly pendingPayments = computed(() =>
    this.paymentTransactionsSignal().filter((transaction) =>
      [PaymentStatus.Initiated, PaymentStatus.Pending].includes(transaction.status as PaymentStatus),
    ),
  );

  async loadPaymentProviders(): Promise<PaymentActionResult> {
    if (!this.permissions.canViewPayments() && !this.permissions.canManagePaymentProviders()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.paymentProvidersSignal.set(await this.api.getPaymentProviders());
    });
  }

  async createPaymentProvider(payload: PaymentProviderPayload): Promise<PaymentActionResult> {
    if (!this.permissions.canManagePaymentProviders()) {
      return this.restricted();
    }

    return this.run(async () => {
      const provider = await this.api.createPaymentProvider(payload);
      this.paymentProvidersSignal.update((providers) => upsert(providers, provider));
    });
  }

  async updatePaymentProvider(
    providerId: string,
    payload: PaymentProviderPayload,
  ): Promise<PaymentActionResult> {
    if (!this.permissions.canManagePaymentProviders()) {
      return this.restricted();
    }

    return this.run(async () => {
      const provider = await this.api.updatePaymentProvider(providerId, payload);
      this.paymentProvidersSignal.update((providers) => upsert(providers, provider));
    });
  }

  async createProviderConfiguration(
    payload: PaymentProviderConfigurationPayload,
  ): Promise<PaymentActionResult> {
    if (!this.permissions.canManagePaymentProviders()) {
      return this.restricted();
    }

    return this.run(async () => {
      const configuration = await this.api.createProviderConfiguration(payload);
      this.providerConfigurationsSignal.update((configurations) =>
        upsert(configurations, configuration),
      );
    });
  }

  async updateProviderConfiguration(
    configurationId: string,
    payload: PaymentProviderConfigurationPayload,
  ): Promise<PaymentActionResult> {
    if (!this.permissions.canManagePaymentProviders()) {
      return this.restricted();
    }

    return this.run(async () => {
      const configuration = await this.api.updateProviderConfiguration(configurationId, payload);
      this.providerConfigurationsSignal.update((configurations) =>
        upsert(configurations, configuration),
      );
    });
  }

  async loadCreditPackages(): Promise<PaymentActionResult> {
    if (!this.permissions.canPurchaseCredits() && !this.permissions.canManageCreditPackages()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.creditPackagesSignal.set(await this.api.getCreditPackages());
    });
  }

  async createCreditPackage(payload: CreditPackagePayload): Promise<PaymentActionResult> {
    if (!this.permissions.canManageCreditPackages()) {
      return this.restricted();
    }

    return this.run(async () => {
      const creditPackage = await this.api.createCreditPackage(payload);
      this.creditPackagesSignal.update((packages) => upsert(packages, creditPackage));
    });
  }

  async updateCreditPackage(
    creditPackageId: string,
    payload: CreditPackagePayload,
  ): Promise<PaymentActionResult> {
    if (!this.permissions.canManageCreditPackages()) {
      return this.restricted();
    }

    return this.run(async () => {
      const creditPackage = await this.api.updateCreditPackage(creditPackageId, payload);
      this.creditPackagesSignal.update((packages) => upsert(packages, creditPackage));
    });
  }

  async purchaseSubscription(
    workspaceId: string,
    payload: SubscriptionPurchasePayload,
  ): Promise<PaymentActionResult> {
    if (!this.permissions.canPurchaseSubscription()) {
      return this.restricted();
    }

    return this.runPaymentSession(() => this.api.purchaseSubscription(workspaceId, payload));
  }

  async upgradeSubscription(
    workspaceId: string,
    payload: SubscriptionUpgradePayload,
  ): Promise<PaymentActionResult> {
    if (!this.permissions.canPurchaseSubscription()) {
      return this.restricted();
    }

    return this.runPaymentSession(() => this.api.upgradeSubscription(workspaceId, payload));
  }

  async renewSubscription(
    workspaceId: string,
    payload: SubscriptionRenewPayload,
  ): Promise<PaymentActionResult> {
    if (!this.permissions.canPurchaseSubscription()) {
      return this.restricted();
    }

    return this.runPaymentSession(() => this.api.renewSubscription(workspaceId, payload));
  }

  async purchaseCredits(
    workspaceId: string,
    payload: CreditPurchasePayload,
  ): Promise<PaymentActionResult> {
    if (!this.permissions.canPurchaseCredits()) {
      return this.restricted();
    }

    return this.runPaymentSession(() => this.api.purchaseCredits(workspaceId, payload));
  }

  async loadWorkspacePayments(
    workspaceId: string,
    filters?: PaymentFilters,
  ): Promise<PaymentActionResult> {
    if (!this.permissions.canViewPayments()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.paymentTransactionsSignal.set(await this.api.getWorkspacePayments(workspaceId, filters));
    });
  }

  async loadWorkspaceInvoices(workspaceId: string): Promise<PaymentActionResult> {
    if (!this.permissions.canViewInvoices()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.invoicesSignal.set(await this.api.getWorkspaceInvoices(workspaceId));
    });
  }

  setSelectedPricingPlan(plan: PricingPlanForPurchase | null): void {
    this.selectedPricingPlanSignal.set(plan);
    this.selectedBillingCycleSignal.set(plan?.billingCycle ?? null);
  }

  setSelectedBillingCycle(cycle: BillingCycle | string | null): void {
    this.selectedBillingCycleSignal.set(cycle);
  }

  setSelectedCreditPackage(creditPackage: CreditPackage | null): void {
    this.selectedCreditPackageSignal.set(creditPackage);
  }

  clearActivePaymentSession(): void {
    this.activePaymentSessionSignal.set(null);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  private async runPaymentSession(
    action: () => Promise<PaymentSessionResponse>,
  ): Promise<PaymentActionResult> {
    return this.run(async () => {
      this.activePaymentSessionSignal.set(await action());
    });
  }

  private async run(action: () => Promise<void>): Promise<PaymentActionResult> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      await action();
      return { ok: true };
    } catch (error) {
      const message = friendlyPaymentError(error);
      this.errorSignal.set(message);
      this.notifications.error('Payments', message);
      return { ok: false, message };
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private restricted(): PaymentActionResult {
    const message = 'You do not have permission to access payment information.';
    this.errorSignal.set(message);
    return { ok: false, message };
  }
}

function upsert<T extends { readonly id: string }>(items: readonly T[], item: T): readonly T[] {
  const index = items.findIndex((current) => current.id === item.id);
  if (index < 0) {
    return [...items, item];
  }

  return items.map((current, currentIndex) => (currentIndex === index ? item : current));
}

function friendlyPaymentError(error: unknown): string {
  const normalized = normalizeHttpError(error);
  return normalized.message || 'We could not load payment information. Please try again.';
}
