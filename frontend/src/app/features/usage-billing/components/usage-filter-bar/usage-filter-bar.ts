import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CreditTransactionType, UsageType } from '../../models/usage-billing.models';

@Component({
  selector: 'app-usage-filter-bar',
  standalone: true,
  imports: [ButtonComponent],
  templateUrl: './usage-filter-bar.html',
  styleUrl: './usage-filter-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsageFilterBarComponent {
  readonly selectedMonth = input<string | null>(null);
  readonly selectedUsageType = input<UsageType | string | null>(null);
  readonly selectedTransactionType = input<CreditTransactionType | string | null>(null);
  readonly loading = input(false);
  readonly showUsageType = input(true);
  readonly showTransactionType = input(true);

  readonly selectedMonthChange = output<string | null>();
  readonly selectedUsageTypeChange = output<UsageType | string | null>();
  readonly selectedTransactionTypeChange = output<CreditTransactionType | string | null>();
  readonly refresh = output<void>();
  readonly clear = output<void>();

  protected readonly usageTypes = Object.values(UsageType);
  protected readonly transactionTypes = Object.values(CreditTransactionType);

  protected updateMonth(value: string): void {
    this.selectedMonthChange.emit(value || null);
  }

  protected updateUsageType(value: string): void {
    this.selectedUsageTypeChange.emit(value || null);
  }

  protected updateTransactionType(value: string): void {
    this.selectedTransactionTypeChange.emit(value || null);
  }

  protected label(value: string): string {
    return value
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }
}
