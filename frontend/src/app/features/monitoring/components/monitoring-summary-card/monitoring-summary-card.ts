import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-monitoring-summary-card',
  standalone: true,
  imports: [CardComponent, IconComponent],
  templateUrl: './monitoring-summary-card.html',
  styleUrl: './monitoring-summary-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonitoringSummaryCardComponent {
  readonly title = input.required<string>();
  readonly value = input<string | number>('0');
  readonly description = input('');
  readonly icon = input('activity');
  readonly tone = input<'neutral' | 'healthy' | 'warning' | 'critical'>('neutral');

  protected readonly iconClasses = computed(() => {
    const base = 'inline-flex size-10 items-center justify-center rounded-md border';
    switch (this.tone()) {
      case 'healthy':
        return `${base} border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300`;
      case 'warning':
        return `${base} border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300`;
      case 'critical':
        return `${base} border-alert-500/30 bg-alert-500/10 text-alert-700 dark:text-alert-300`;
      default:
        return `${base} border-border bg-panel text-muted`;
    }
  });
}
