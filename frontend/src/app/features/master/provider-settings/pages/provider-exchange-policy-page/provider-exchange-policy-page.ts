import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { CreditValuePolicyPayload, FreeSignupCreditMode } from '../../models/provider-credit-exchange.models';
import { ExchangePolicyFormComponent } from '../../components/exchange-policy-form/exchange-policy-form';
import { FreeCreditPreviewCardComponent } from '../../components/free-credit-preview-card/free-credit-preview-card';
import { ExchangePolicyStore } from '../../state/exchangePolicy.store';
import { ProviderCreditsStore } from '../../state/providerCredits.store';
import { ProviderSettingsStore } from '../../state/providerSettings.store';

@Component({ selector: 'app-provider-exchange-policy-page', standalone: true, imports: [ReactiveFormsModule, ButtonComponent, AppErrorStateComponent, PageHeaderComponent, ExchangePolicyFormComponent, FreeCreditPreviewCardComponent], templateUrl: './provider-exchange-policy-page.html', styleUrl: './provider-exchange-policy-page.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class ProviderExchangePolicyPage {
  protected readonly providers = inject(ProviderSettingsStore);
  protected readonly credits = inject(ProviderCreditsStore);
  protected readonly policies = inject(ExchangePolicyStore);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly creditValueFormChanged = signal(0);
  protected readonly freeSignupModes: readonly FreeSignupCreditMode[] = ['FIXED_CREDITS', 'FIXED_USD_VALUE', 'PERCENTAGE_OF_PROVIDER_POOL'];
  protected readonly creditValueForm = new FormGroup({
    currency: new FormControl('USD', { nonNullable: true, validators: [Validators.required] }),
    creditUsdValue: new FormControl(0.05, { nonNullable: true, validators: [Validators.required, Validators.min(0.000001)] }),
    averageProviderCostPerCreativeUsd: new FormControl(0.19, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    providerCostMultiplier: new FormControl(5, { nonNullable: true, validators: [Validators.required, Validators.min(1)] }),
    freeSignupCreditEnabled: new FormControl(true, { nonNullable: true }),
    freeSignupMode: new FormControl<FreeSignupCreditMode>('FIXED_CREDITS', { nonNullable: true, validators: [Validators.required] }),
    freeSignupCredits: new FormControl(25, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    freeSignupUsdValue: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    freeSignupPercentage: new FormControl(2, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(100)] }),
    oneTimePerWorkspace: new FormControl(true, { nonNullable: true }),
    minimumWalletBalanceWarning: new FormControl(5, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    active: new FormControl(true, { nonNullable: true }),
    effectiveFrom: new FormControl('', { nonNullable: true }),
  });
  protected readonly localCreditPreview = computed(() => {
    this.creditValueFormChanged();
    const value = this.creditValueForm.getRawValue();
    const creativeCostUsd = value.averageProviderCostPerCreativeUsd * value.providerCostMultiplier;
    const creativeCredits = value.creditUsdValue > 0 ? Math.ceil(creativeCostUsd / value.creditUsdValue) : 0;
    const freeCredits = this.calculateFreeCredits(value);
    return {
      creativeCostUsd,
      creativeCredits,
      freeCredits,
      freeSignupUsdEquivalent: freeCredits * value.creditUsdValue,
    };
  });

  constructor() {
    effect(() => void this.providers.loadProviders());
    effect(() => void this.policies.loadCreditValuePolicy());
    effect(() => {
      const policy = this.policies.creditValuePolicy();
      if (!policy) {
        return;
      }
      this.creditValueForm.patchValue({
        currency: policy.currency,
        creditUsdValue: policy.creditUsdValue,
        averageProviderCostPerCreativeUsd: policy.averageProviderCostPerCreativeUsd,
        providerCostMultiplier: policy.providerCostMultiplier,
        freeSignupCreditEnabled: policy.freeSignupCreditEnabled,
        freeSignupMode: policy.freeSignupMode,
        freeSignupCredits: policy.freeSignupCredits,
        freeSignupUsdValue: policy.freeSignupUsdValue,
        freeSignupPercentage: policy.freeSignupPercentage,
        oneTimePerWorkspace: policy.oneTimePerWorkspace,
        minimumWalletBalanceWarning: policy.minimumWalletBalanceWarning,
        active: policy.active,
        effectiveFrom: policy.effectiveFrom ? policy.effectiveFrom.slice(0, 16) : '',
      }, { emitEvent: false });
      this.creditValueFormChanged.update((value) => value + 1);
    });
    this.creditValueForm.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.creditValueFormChanged.update((value) => value + 1);
    });
    effect(() => {
      const providerId = this.route.snapshot.queryParamMap.get('providerId') ?? this.providers.selectedProvider()?.id;
      if (providerId) {
        this.providers.setSelectedProvider(providerId);
        void this.policies.loadPolicy(providerId);
        void this.credits.loadPool(providerId).then(() => this.policies.setPreviewPool(this.credits.selectedProviderPool()));
      }
    });
  }

  protected selectProvider(providerId: string): void { this.providers.setSelectedProvider(providerId); void this.policies.loadPolicy(providerId); void this.credits.loadPool(providerId).then(() => this.policies.setPreviewPool(this.credits.selectedProviderPool())); }

  protected saveCreditValuePolicy(): void {
    if (this.creditValueForm.invalid) {
      this.creditValueForm.markAllAsTouched();
      return;
    }
    void this.policies.saveCreditValuePolicy(this.creditPolicyPayload());
  }

  protected previewCreditValuePolicy(): void {
    if (this.creditValueForm.invalid) {
      return;
    }
    void this.policies.previewCreditValuePolicy(this.creditPolicyPayload());
  }

  private creditPolicyPayload(): CreditValuePolicyPayload {
    const value = this.creditValueForm.getRawValue();
    return {
      ...value,
      currency: value.currency || 'USD',
      effectiveFrom: value.effectiveFrom ? new Date(value.effectiveFrom).toISOString() : null,
    };
  }

  private calculateFreeCredits(value: ReturnType<typeof this.creditValueForm.getRawValue>): number {
    if (!value.freeSignupCreditEnabled) {
      return 0;
    }
    if (value.freeSignupMode === 'FIXED_CREDITS') {
      return value.freeSignupCredits;
    }
    if (value.freeSignupMode === 'FIXED_USD_VALUE') {
      return value.creditUsdValue > 0 ? Math.floor(value.freeSignupUsdValue / value.creditUsdValue) : 0;
    }
    const available = this.policies.freeCreditPreview().available;
    return Math.floor((available * value.freeSignupPercentage) / 100);
  }
}
