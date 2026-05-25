import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

type BadgeTone = 'neutral' | 'brand' | 'blue' | 'red';

@Component({
  selector: 'app-status-badge, app-badge',
  standalone: true,
  templateUrl: './app-status-badge.html',
  styleUrl: './app-status-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BadgeComponent {
  readonly tone = input<BadgeTone>('neutral');

  protected readonly classes = computed(() => {
    const tones: Record<BadgeTone, string> = {
      neutral: 'bg-panel text-ink',
      brand: 'bg-brand-100 text-brand-700 dark:bg-brand-500/20 dark:text-brand-100',
      blue: 'bg-blue-100 text-blue-700 dark:bg-blue-500/20 dark:text-blue-100',
      red: 'bg-red-100 text-red-700 dark:bg-red-500/20 dark:text-red-100',
    };

    return `inline-flex h-6 items-center rounded-full px-2.5 text-xs font-medium ${tones[this.tone()]}`;
  });
}
