import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { IconComponent } from '@app/shared/components/icon/icon';
import { AiProviderView } from '../../models/provider-credit-exchange.models';
import { ProviderStatusBadgeComponent } from '../provider-status-badge/provider-status-badge';

@Component({
  selector: 'app-provider-card',
  standalone: true,
  imports: [ButtonComponent, IconComponent, ProviderStatusBadgeComponent],
  templateUrl: './provider-card.html',
  styleUrl: './provider-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderCardComponent {
  readonly provider = input.required<AiProviderView>();
  readonly selected = input(false);
  readonly selectProvider = output<AiProviderView>();
  readonly configureCredentials = output<AiProviderView>();
  readonly editProvider = output<AiProviderView>();
  readonly toggleProvider = output<AiProviderView>();
  readonly deleteProvider = output<AiProviderView>();
  readonly testProvider = output<AiProviderView>();
  readonly viewModelsJson = output<AiProviderView>();
  readonly viewCreditPool = output<AiProviderView>();
  readonly exchangePolicy = output<AiProviderView>();

  protected canShowModelsJson(provider: AiProviderView): boolean {
    const providerType = (provider.providerType || provider.category || '').toUpperCase();
    const providerCode = provider.providerCode.toUpperCase();
    return Boolean(provider.modelsEndpoint?.trim())
      && providerType !== 'PAYMENT'
      && !['STRIPE', 'SSLCOMMERZ', 'BKASH', 'NAGAD'].includes(providerCode);
  }
}
