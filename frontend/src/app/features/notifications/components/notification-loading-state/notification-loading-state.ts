import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-notification-loading-state',
  standalone: true,
  templateUrl: './notification-loading-state.html',
  styleUrl: './notification-loading-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationLoadingStateComponent {
  readonly label = input('Loading notifications');
  readonly cardCount = input(4);

  protected readonly skeletonCards = Array.from({ length: 8 }, (_, index) => index);
}
