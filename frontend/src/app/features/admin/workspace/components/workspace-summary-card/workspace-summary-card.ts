import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/card/card';
import { IconComponent } from '@app/shared/components/icon/icon';

type SummaryAccent = 'brand' | 'blue' | 'neutral';

@Component({
  selector: 'app-workspace-summary-card',
  standalone: true,
  imports: [CardComponent, IconComponent],
  templateUrl: './workspace-summary-card.html',
  styleUrl: './workspace-summary-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceSummaryCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly description = input('');
  readonly icon = input.required<string>();
  readonly accent = input<SummaryAccent>('brand');

  protected readonly iconContainerClasses = computed(() => {
    const accentClasses: Record<SummaryAccent, string> = {
      brand: 'bg-brand-50 text-brand-700',
      blue: 'bg-blue-50 text-blue-700',
      neutral: 'bg-slate-100 text-slate-700',
    };

    return `grid h-11 w-11 shrink-0 place-items-center rounded-lg ${accentClasses[this.accent()]}`;
  });
}
