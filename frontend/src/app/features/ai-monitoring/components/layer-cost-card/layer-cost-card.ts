import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { AiLayerAnalytics } from '../../models/ai-monitoring.models';

@Component({
  selector: 'app-layer-cost-card',
  standalone: true,
  imports: [CurrencyPipe, CardComponent],
  templateUrl: './layer-cost-card.html',
  styleUrl: './layer-cost-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LayerCostCardComponent {
  readonly layer = input.required<AiLayerAnalytics>();

  protected readonly estimatedCost = computed(
    () => this.layer().avgExecutionCostUsd * this.layer().totalExecutions,
  );
}
