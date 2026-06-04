import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CreditLedgerItemView, creditTransactionLabel } from '../../models/credits.models';

@Component({ selector: 'app-credit-ledger-table', standalone: true, imports: [DatePipe, DecimalPipe], templateUrl: './credit-ledger-table.html', styleUrl: './credit-ledger-table.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class CreditLedgerTableComponent {
  readonly ledger = input<readonly CreditLedgerItemView[]>([]);
  protected label(type: string): string { return creditTransactionLabel(type); }
}
