import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { CreditPackage } from '../../models/payment.models';

type CreditPackageWithBadge = CreditPackage & { readonly bestValue?: boolean };

@Component({
  selector: 'app-credit-purchase-card',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, ButtonComponent, CardComponent, BadgeComponent],
  templateUrl: './credit-purchase-card.html',
  styleUrl: './credit-purchase-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreditPurchaseCardComponent {
  readonly creditPackage = input.required<CreditPackageWithBadge>();
  readonly selected = input(false);
  readonly selectPackage = output<CreditPackage>();

  protected readonly totalCredits = computed(
    () => this.creditPackage().credits + this.creditPackage().bonusCredits,
  );
}
