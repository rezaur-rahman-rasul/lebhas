import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { IconComponent } from '@app/shared/components/icon/icon';
import { WorkspaceUsageSummary } from '../../models/usage-billing.models';

@Component({
  selector: 'app-credit-balance-card',
  standalone: true,
  imports: [DecimalPipe, CardComponent, IconComponent],
  templateUrl: './credit-balance-card.html',
  styleUrl: './credit-balance-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreditBalanceCardComponent {
  readonly summary = input.required<WorkspaceUsageSummary>();
  readonly availableCredits = input<number | null>(null);

  protected readonly displayedAvailableCredits = computed(() => {
    const provided = this.availableCredits();
    if (typeof provided === 'number') {
      return provided;
    }

    return this.summary().creditLimit?.remaining ?? null;
  });
}
