import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-card',
  standalone: true,
  templateUrl: './app-card.html',
  styleUrl: './app-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardComponent {
  readonly padded = input(true);
  readonly interactive = input(false);

  protected readonly classes = computed(() =>
    [
      'ui-glass-card overflow-hidden',
      this.padded() ? 'p-[var(--app-card-padding)]' : '',
      this.interactive() ? 'transition hover:-translate-y-0.5 hover:shadow-soft' : '',
    ]
      .filter(Boolean)
      .join(' '),
  );
}
