import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';

@Component({
  selector: 'app-payment-empty-state',
  standalone: true,
  imports: [ButtonComponent, EmptyStateComponent],
  templateUrl: './payment-empty-state.html',
  styleUrl: './payment-empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentEmptyStateComponent {
  readonly title = input('No payment history yet');
  readonly description = input('Your successful payments will appear here.');
  readonly icon = input('receipt-text');
  readonly retryLabel = input<string | null>(null);
  readonly retry = output<void>();
}
