import { Injectable, computed, inject, signal } from '@angular/core';
import { normalizeHttpError } from '@app/core/api/http-error';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  CreditValuePolicyPayload,
  CreditValuePolicyView,
  ProviderCreditExchangePolicyView,
  ProviderCreditPoolView,
} from '../models/provider-credit-exchange.models';
import { ExchangePolicyApiService } from '../services/exchange-policy-api.service';

@Injectable({ providedIn: 'root' })
export class ExchangePolicyStore {
  private readonly api = inject(ExchangePolicyApiService);
  private readonly notifications = inject(NotificationStateService);

  private readonly policiesSignal = signal<readonly ProviderCreditExchangePolicyView[]>([]);
  private readonly selectedPolicySignal = signal<ProviderCreditExchangePolicyView | null>(null);
  private readonly selectedPoolSignal = signal<ProviderCreditPoolView | null>(null);
  private readonly creditValuePolicySignal = signal<CreditValuePolicyView | null>(null);
  private readonly creditValuePreviewSignal = signal<CreditValuePolicyView | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly savingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly policies = this.policiesSignal.asReadonly();
  readonly selectedPolicy = this.selectedPolicySignal.asReadonly();
  readonly creditValuePolicy = this.creditValuePolicySignal.asReadonly();
  readonly creditValuePreview = this.creditValuePreviewSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly saving = this.savingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly activePolicies = computed(() => this.policiesSignal().filter((policy) => policy.active));
  readonly freeCreditPreview = computed(() => calculatePreview(this.selectedPolicySignal(), this.selectedPoolSignal()));

  setPreviewPool(pool: ProviderCreditPoolView | null): void {
    this.selectedPoolSignal.set(pool);
  }

  async loadPolicy(providerId: string): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    try {
      const policy = await this.api.getExchangePolicy(providerId);
      this.selectedPolicySignal.set(policy);
      this.policiesSignal.update((items) => upsert(items, policy));
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message || 'Exchange policy could not be loaded.');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async savePolicy(providerId: string, payload: ProviderCreditExchangePolicyView): Promise<boolean> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);
    try {
      const saved = this.selectedPolicySignal()
        ? await this.api.updateExchangePolicy(providerId, payload)
        : await this.api.createExchangePolicy(providerId, payload);
      this.selectedPolicySignal.set(saved);
      this.policiesSignal.update((items) => upsert(items, saved));
      this.notifications.success('Exchange policy updated.', 'Exchange policy updated.');
      return true;
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message || 'Exchange policy could not be updated.');
      this.notifications.error('Exchange policy could not be updated.', 'Exchange policy could not be updated.');
      return false;
    } finally {
      this.savingSignal.set(false);
    }
  }

  async loadCreditValuePolicy(): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    try {
      const policy = await this.api.getCreditValuePolicy();
      this.creditValuePolicySignal.set(policy);
      this.creditValuePreviewSignal.set(policy);
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message || 'Credit value policy could not be loaded.');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async saveCreditValuePolicy(payload: CreditValuePolicyPayload): Promise<boolean> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);
    try {
      const saved = await this.api.updateCreditValuePolicy(payload);
      this.creditValuePolicySignal.set(saved);
      this.creditValuePreviewSignal.set(saved);
      this.notifications.success('Credit value policy updated.', 'Credit value policy updated.');
      return true;
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message || 'Credit value policy could not be updated.');
      this.notifications.error('Credit value policy could not be updated.', 'Credit value policy could not be updated.');
      return false;
    } finally {
      this.savingSignal.set(false);
    }
  }

  async previewCreditValuePolicy(payload: CreditValuePolicyPayload): Promise<void> {
    try {
      this.creditValuePreviewSignal.set(await this.api.previewCreditValuePolicy(payload));
    } catch {
      this.creditValuePreviewSignal.set(null);
    }
  }
}

function calculatePreview(policy: ProviderCreditExchangePolicyView | null, pool: ProviderCreditPoolView | null) {
  const available = pool?.availableInternalCredits ?? 0;
  const percentage = policy?.freeSignupCreditPercentage ?? 0;
  const estimated = policy?.freeSignupCreditEnabled ? Math.floor((available * percentage) / 100) : 0;
  const capped = Math.min(estimated, policy?.maxFreeSignupCredits ?? estimated);
  const result = available >= (policy?.minProviderBalanceRequired ?? 0) ? capped : (policy?.fallbackFreeCredits ?? 0);
  return { available, percentage, estimated, maxCap: policy?.maxFreeSignupCredits ?? 0, fallback: policy?.fallbackFreeCredits ?? 0, result };
}

function upsert(items: readonly ProviderCreditExchangePolicyView[], policy: ProviderCreditExchangePolicyView): readonly ProviderCreditExchangePolicyView[] {
  return items.some((item) => item.providerId === policy.providerId) ? items.map((item) => item.providerId === policy.providerId ? policy : item) : [...items, policy];
}
