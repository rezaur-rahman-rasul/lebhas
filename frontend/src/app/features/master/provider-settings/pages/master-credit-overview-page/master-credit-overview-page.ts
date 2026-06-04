import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ProviderCreditsStore } from '../../state/providerCredits.store';

@Component({ selector: 'app-master-credit-overview-page', standalone: true, imports: [DatePipe, DecimalPipe, ButtonComponent, AppErrorStateComponent, PageHeaderComponent], templateUrl: './master-credit-overview-page.html', styleUrl: './master-credit-overview-page.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class MasterCreditOverviewPage {
  protected readonly credits = inject(ProviderCreditsStore);
  constructor() { effect(() => void this.credits.loadOverview()); }
}
