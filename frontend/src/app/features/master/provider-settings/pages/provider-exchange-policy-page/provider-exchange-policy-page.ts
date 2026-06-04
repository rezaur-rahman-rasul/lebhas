import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ExchangePolicyFormComponent } from '../../components/exchange-policy-form/exchange-policy-form';
import { FreeCreditPreviewCardComponent } from '../../components/free-credit-preview-card/free-credit-preview-card';
import { ExchangePolicyStore } from '../../state/exchangePolicy.store';
import { ProviderCreditsStore } from '../../state/providerCredits.store';
import { ProviderSettingsStore } from '../../state/providerSettings.store';

@Component({ selector: 'app-provider-exchange-policy-page', standalone: true, imports: [ButtonComponent, AppErrorStateComponent, PageHeaderComponent, ExchangePolicyFormComponent, FreeCreditPreviewCardComponent], templateUrl: './provider-exchange-policy-page.html', styleUrl: './provider-exchange-policy-page.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class ProviderExchangePolicyPage {
  protected readonly providers = inject(ProviderSettingsStore);
  protected readonly credits = inject(ProviderCreditsStore);
  protected readonly policies = inject(ExchangePolicyStore);
  private readonly route = inject(ActivatedRoute);

  constructor() {
    effect(() => void this.providers.loadProviders());
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
}
