import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { PaymentProvider } from '../../models/payment.models';

@Component({
  selector: 'app-payment-provider-card',
  standalone: true,
  imports: [DatePipe, ButtonComponent, CardComponent, BadgeComponent],
  templateUrl: './payment-provider-card.html',
  styleUrl: './payment-provider-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentProviderCardComponent {
  readonly provider = input.required<PaymentProvider>();
  readonly edit = output<PaymentProvider>();
  readonly configure = output<PaymentProvider>();

  protected readonly statusTone = computed(() => (this.provider().isEnabled ? 'brand' : 'neutral'));
  protected readonly statusLabel = computed(() =>
    this.provider().isEnabled ? 'Enabled' : 'Disabled',
  );
}
