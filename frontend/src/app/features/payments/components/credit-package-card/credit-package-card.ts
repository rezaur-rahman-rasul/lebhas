import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { CreditPackage } from '../../models/payment.models';

@Component({
  selector: 'app-credit-package-card',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, ButtonComponent, CardComponent, BadgeComponent],
  templateUrl: './credit-package-card.html',
  styleUrl: './credit-package-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreditPackageCardComponent {
  readonly creditPackage = input.required<CreditPackage>();
  readonly edit = output<CreditPackage>();
  readonly toggleActive = output<CreditPackage>();
}
