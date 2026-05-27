import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { PaymentPurpose } from '../../models/payment.models';

@Component({
  selector: 'app-payment-purpose-badge',
  standalone: true,
  imports: [BadgeComponent],
  templateUrl: './payment-purpose-badge.html',
  styleUrl: './payment-purpose-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentPurposeBadgeComponent {
  readonly purpose = input.required<PaymentPurpose | string>();

  protected readonly label = computed(() => {
    switch (this.purpose()) {
      case PaymentPurpose.SubscriptionPurchase:
        return 'Subscription purchase';
      case PaymentPurpose.PlanUpgrade:
        return 'Plan upgrade';
      case PaymentPurpose.PlanRenewal:
        return 'Plan renewal';
      case PaymentPurpose.CreditPurchase:
        return 'Credit purchase';
      default:
        return this.purpose()
          .toLowerCase()
          .replace(/_/g, ' ')
          .replace(/\b\w/g, (letter) => letter.toUpperCase());
    }
  });
}
