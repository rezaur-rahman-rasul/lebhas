import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';

@Component({
  selector: 'app-notification-empty-state',
  standalone: true,
  imports: [ButtonComponent, EmptyStateComponent],
  templateUrl: './notification-empty-state.html',
  styleUrl: './notification-empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationEmptyStateComponent {
  readonly title = input('No notifications yet');
  readonly description = input('Important updates will appear here.');
  readonly icon = input('bell');
  readonly retryLabel = input<string | null>(null);

  readonly retry = output<void>();
}
