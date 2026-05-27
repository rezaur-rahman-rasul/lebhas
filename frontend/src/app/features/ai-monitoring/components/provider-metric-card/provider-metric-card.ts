import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { IconComponent } from '@app/shared/components/icon/icon';
import { AiProviderMetric } from '../../models/ai-monitoring.models';

@Component({
  selector: 'app-provider-metric-card',
  standalone: true,
  imports: [CurrencyPipe, CardComponent, IconComponent],
  templateUrl: './provider-metric-card.html',
  styleUrl: './provider-metric-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderMetricCardComponent {
  readonly metric = input.required<AiProviderMetric>();

  protected readonly successRate = computed(() => {
    const total = this.metric().totalRequests;
    return total ? Math.round((this.metric().successfulRequests / total) * 100) : 0;
  });
}
