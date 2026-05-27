import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { CreditLedger, CreditTransactionType } from '../../models/usage-billing.models';

@Component({
  selector: 'app-credit-ledger-card',
  standalone: true,
  imports: [DatePipe, DecimalPipe, CardComponent, BadgeComponent],
  templateUrl: './credit-ledger-card.html',
  styleUrl: './credit-ledger-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreditLedgerCardComponent {
  readonly entry = input.required<CreditLedger>();

  protected readonly transactionLabel = computed(() => this.toTitleCase(this.entry().transactionType));

  protected readonly explanation = computed(() => {
    switch (this.entry().transactionType) {
      case CreditTransactionType.Reserve:
        return 'Credits are temporarily held for generation.';
      case CreditTransactionType.Finalize:
        return 'Credits were used after successful generation.';
      case CreditTransactionType.Refund:
        return 'Credits returned after failed generation.';
      default:
        return this.entry().description || 'Credit balance was updated.';
    }
  });

  protected readonly tone = computed(() => {
    switch (this.entry().transactionType) {
      case CreditTransactionType.Refund:
        return 'brand';
      case CreditTransactionType.Expiry:
        return 'red';
      case CreditTransactionType.Reserve:
        return 'blue';
      default:
        return 'neutral';
    }
  });

  protected readonly amountPrefix = computed(() =>
    this.entry().transactionType === CreditTransactionType.Refund ? '+' : '',
  );

  protected toTitleCase(value: string | null): string {
    return (value || 'Credit update')
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }
}
