import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ProviderCreditPoolView } from '../../models/provider-credit-exchange.models';

@Component({
  selector: 'app-provider-credit-pool-card',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './provider-credit-pool-card.html',
  styleUrl: './provider-credit-pool-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderCreditPoolCardComponent {
  readonly pool = input.required<ProviderCreditPoolView>();
}
