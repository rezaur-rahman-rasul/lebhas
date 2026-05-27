import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { MonthlyUsageSnapshot } from '../../models/usage-billing.models';

@Component({
  selector: 'app-monthly-snapshot-card',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, CardComponent],
  templateUrl: './monthly-snapshot-card.html',
  styleUrl: './monthly-snapshot-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthlySnapshotCardComponent {
  readonly snapshot = input.required<MonthlyUsageSnapshot>();

  protected formatBytes(value: number): string {
    if (value <= 0) {
      return '0 B';
    }

    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const unitIndex = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1);
    const size = value / 1024 ** unitIndex;
    return `${size.toFixed(size >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
  }
}
