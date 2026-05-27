import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-unread-count-pill',
  standalone: true,
  templateUrl: './unread-count-pill.html',
  styleUrl: './unread-count-pill.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UnreadCountPillComponent {
  readonly count = input(0);
  readonly compact = input(false);

  protected readonly label = computed(() => {
    const count = this.count();
    if (count <= 0) {
      return 'No unread notifications';
    }

    return `${count} unread notification${count === 1 ? '' : 's'}`;
  });

  protected readonly displayCount = computed(() => (this.count() > 99 ? '99+' : String(this.count())));
}
