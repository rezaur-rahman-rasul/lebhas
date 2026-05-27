import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-usage-progress-bar',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './usage-progress-bar.html',
  styleUrl: './usage-progress-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsageProgressBarComponent {
  readonly label = input.required<string>();
  readonly current = input.required<number>();
  readonly limit = input<number | null>(null);
  readonly percent = input<number | null>(null);
  readonly nearLimit = input(false);
  readonly exceeded = input(false);
  readonly helperText = input<string | null>(null);

  protected readonly hasLimit = computed(() => {
    const limit = this.limit();
    return typeof limit === 'number' && limit > 0;
  });

  protected readonly displayPercent = computed(() => {
    const explicit = this.percent();
    if (typeof explicit === 'number') {
      return Math.max(0, Math.min(explicit, 100));
    }

    const limit = this.limit();
    if (!limit || limit <= 0) {
      return null;
    }

    return Math.max(0, Math.min((this.current() / limit) * 100, 100));
  });

  protected readonly barClasses = computed(() => {
    if (this.exceeded()) {
      return 'bg-red-500';
    }

    if (this.nearLimit()) {
      return 'bg-amber-500';
    }

    return 'bg-brand-500';
  });

  protected readonly statusText = computed(() => {
    if (!this.hasLimit()) {
      return 'No package limit provided';
    }

    if (this.exceeded()) {
      return 'Your current package limit has been reached.';
    }

    if (this.nearLimit()) {
      return 'You are close to your current package limit.';
    }

    return this.helperText() ?? 'Within your current package limit.';
  });
}
