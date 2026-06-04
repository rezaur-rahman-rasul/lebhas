import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CreditLedgerItemView } from '../../models/provider-credit-exchange.models';

@Component({
  selector: 'app-provider-credit-ledger-table',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './provider-credit-ledger-table.html',
  styleUrl: './provider-credit-ledger-table.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderCreditLedgerTableComponent {
  readonly ledger = input<readonly CreditLedgerItemView[]>([]);
}
