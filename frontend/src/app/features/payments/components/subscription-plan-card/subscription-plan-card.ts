import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { PricingPlanForPurchase } from '../../models/payment.models';

@Component({
  selector: 'app-subscription-plan-card',
  standalone: true,
  imports: [CurrencyPipe, ButtonComponent, CardComponent, BadgeComponent],
  templateUrl: './subscription-plan-card.html',
  styleUrl: './subscription-plan-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubscriptionPlanCardComponent {
  readonly plan = input.required<PricingPlanForPurchase>();
  readonly selected = input(false);
  readonly selectPlan = output<PricingPlanForPurchase>();

  protected readonly featureEntries = computed(() => Object.entries(this.plan().features ?? {}));
  protected readonly limitEntries = computed(() => Object.entries(this.plan().limits ?? {}));
}
