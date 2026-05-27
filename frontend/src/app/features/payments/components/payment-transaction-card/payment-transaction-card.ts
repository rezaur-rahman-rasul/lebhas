import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { PaymentTransaction } from '../../models/payment.models';
import { maskProviderTransactionId } from '../../services/payment-security';
import { PaymentPurposeBadgeComponent } from '../payment-purpose-badge/payment-purpose-badge';
import { PaymentStatusBadgeComponent } from '../payment-status-badge/payment-status-badge';

@Component({
  selector: 'app-payment-transaction-card',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, CardComponent, PaymentPurposeBadgeComponent, PaymentStatusBadgeComponent],
  templateUrl: './payment-transaction-card.html',
  styleUrl: './payment-transaction-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentTransactionCardComponent {
  readonly transaction = input.required<PaymentTransaction>();

  protected readonly maskedProviderTransactionId = computed(() =>
    maskProviderTransactionId(this.transaction().providerTransactionId),
  );
}
