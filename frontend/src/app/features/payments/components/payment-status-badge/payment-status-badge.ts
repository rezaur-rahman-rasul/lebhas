import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { PaymentStatus } from '../../models/payment.models';

@Component({
  selector: 'app-payment-status-badge',
  standalone: true,
  imports: [BadgeComponent],
  templateUrl: './payment-status-badge.html',
  styleUrl: './payment-status-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentStatusBadgeComponent {
  readonly status = input.required<PaymentStatus | string>();

  protected readonly label = computed(() => {
    switch (this.status()) {
      case PaymentStatus.Initiated:
        return 'Payment started';
      case PaymentStatus.Pending:
        return 'Waiting for payment confirmation';
      case PaymentStatus.Success:
        return 'Payment successful';
      case PaymentStatus.Failed:
        return 'Payment failed';
      case PaymentStatus.Cancelled:
        return 'Payment cancelled';
      case PaymentStatus.Expired:
        return 'Payment session expired';
      case PaymentStatus.Refunded:
        return 'Payment refunded';
      default:
        return toTitleCase(this.status());
    }
  });

  protected readonly tone = computed(() => {
    switch (this.status()) {
      case PaymentStatus.Success:
      case PaymentStatus.Refunded:
        return 'brand';
      case PaymentStatus.Failed:
      case PaymentStatus.Cancelled:
      case PaymentStatus.Expired:
        return 'red';
      case PaymentStatus.Pending:
      case PaymentStatus.Initiated:
        return 'blue';
      default:
        return 'neutral';
    }
  });
}

function toTitleCase(value: string): string {
  return value
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}
