import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-card',
  standalone: true,
  templateUrl: './card.html',
  styleUrl: './card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardComponent {
  readonly padded = input(true);
  readonly interactive = input(false);

  protected readonly classes = computed(() =>
    [
      'overflow-hidden rounded-lg border border-border bg-surface shadow-[0_18px_50px_rgba(2,6,23,0.12)]',
      this.padded() ? 'p-5' : '',
      this.interactive() ? 'transition hover:-translate-y-0.5 hover:shadow-soft' : '',
    ]
      .filter(Boolean)
      .join(' '),
  );
}
