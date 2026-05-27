import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { Invoice } from '../../models/payment.models';
import { PaymentStatusBadgeComponent } from '../payment-status-badge/payment-status-badge';

@Component({
  selector: 'app-invoice-card',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, ButtonComponent, CardComponent, PaymentStatusBadgeComponent],
  templateUrl: './invoice-card.html',
  styleUrl: './invoice-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvoiceCardComponent {
  readonly invoice = input.required<Invoice>();
  readonly downloadAvailable = input(false);
  readonly download = output<Invoice>();
}
