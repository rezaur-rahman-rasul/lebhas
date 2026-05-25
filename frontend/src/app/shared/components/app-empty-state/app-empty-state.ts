import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { IconComponent } from '../icon/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './app-empty-state.html',
  styleUrl: './app-empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  readonly icon = input('sparkles');
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly tone = input<'neutral' | 'brand' | 'danger'>('neutral');

  protected readonly iconClasses = computed(() => {
    const tones: Record<'neutral' | 'brand' | 'danger', string> = {
      neutral: 'bg-panel text-muted',
      brand: 'bg-brand-100 text-brand-700 dark:bg-brand-500/20 dark:text-brand-100',
      danger: 'bg-red-100 text-red-700 dark:bg-red-500/20 dark:text-red-100',
    };

    return `mx-auto grid h-12 w-12 place-items-center rounded-full ${tones[this.tone()]}`;
  });
}
