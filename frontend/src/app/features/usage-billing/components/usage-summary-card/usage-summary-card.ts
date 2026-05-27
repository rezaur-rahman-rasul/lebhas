import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { IconComponent } from '@app/shared/components/icon/icon';

type UsageSummaryFormat = 'number' | 'currency' | 'bytes';
type UsageSummaryTone = 'neutral' | 'brand' | 'warning' | 'danger';

@Component({
  selector: 'app-usage-summary-card',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, CardComponent, IconComponent],
  templateUrl: './usage-summary-card.html',
  styleUrl: './usage-summary-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsageSummaryCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<number>();
  readonly helperText = input<string | null>(null);
  readonly icon = input('bar-chart-3');
  readonly format = input<UsageSummaryFormat>('number');
  readonly tone = input<UsageSummaryTone>('neutral');

  protected readonly iconClasses = computed(() => {
    const tones: Record<UsageSummaryTone, string> = {
      neutral: 'bg-panel text-muted',
      brand: 'bg-brand-100 text-brand-700 dark:bg-brand-500/20 dark:text-brand-100',
      warning: 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-100',
      danger: 'bg-red-100 text-red-700 dark:bg-red-500/20 dark:text-red-100',
    };

    return `grid h-10 w-10 place-items-center rounded-lg ${tones[this.tone()]}`;
  });

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
