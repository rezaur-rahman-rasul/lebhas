import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ProviderCreditAdjustmentDialogComponent } from '../../components/provider-credit-adjustment-dialog/provider-credit-adjustment-dialog';
import { ProviderCreditLedgerTableComponent } from '../../components/provider-credit-ledger-table/provider-credit-ledger-table';
import { ProviderCreditPoolCardComponent } from '../../components/provider-credit-pool-card/provider-credit-pool-card';
import { ProviderCreditsStore } from '../../state/providerCredits.store';
import { ProviderSettingsStore } from '../../state/providerSettings.store';

@Component({ selector: 'app-provider-credit-pool-page', standalone: true, imports: [ButtonComponent, AppErrorStateComponent, PageHeaderComponent, ProviderCreditPoolCardComponent, ProviderCreditAdjustmentDialogComponent, ProviderCreditLedgerTableComponent], templateUrl: './provider-credit-pool-page.html', styleUrl: './provider-credit-pool-page.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class ProviderCreditPoolPage {
  protected readonly providers = inject(ProviderSettingsStore);
  protected readonly credits = inject(ProviderCreditsStore);
  private readonly route = inject(ActivatedRoute);
  protected readonly adjustmentOpen = signal(false);

  constructor() {
    effect(() => void this.providers.loadProviders());
    effect(() => {
      const providerId = this.route.snapshot.queryParamMap.get('providerId') ?? this.providers.selectedProvider()?.id;
      if (providerId) {
        this.providers.setSelectedProvider(providerId);
        void this.credits.loadPool(providerId);
      }
    });
  }

  protected selectProvider(providerId: string): void { this.providers.setSelectedProvider(providerId); void this.credits.loadPool(providerId); }
  protected adjust(payload: Parameters<ProviderCreditsStore['adjustProviderCreditPool']>[1]): void { const providerId = this.providers.selectedProvider()?.id; if (providerId) void this.credits.adjustProviderCreditPool(providerId, payload).then((ok) => ok && this.adjustmentOpen.set(false)); }
}

