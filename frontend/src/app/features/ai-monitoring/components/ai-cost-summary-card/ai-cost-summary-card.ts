import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-ai-cost-summary-card',
  standalone: true,
  imports: [CurrencyPipe, CardComponent, IconComponent],
  templateUrl: './ai-cost-summary-card.html',
  styleUrl: './ai-cost-summary-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiCostSummaryCardComponent {
  readonly title = input('AI cost');
  readonly amount = input.required<number>();
  readonly helperText = input<string | null>(null);
  readonly icon = input('wallet-cards');
}
