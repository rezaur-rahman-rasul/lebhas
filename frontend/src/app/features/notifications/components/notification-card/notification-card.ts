import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { Notification, NotificationPriority } from '../../models/notification.models';
import { NotificationPriorityBadgeComponent } from '../notification-priority-badge/notification-priority-badge';
import {
  NotificationTypeIconComponent,
  friendlyNotificationType,
} from '../notification-type-icon/notification-type-icon';

@Component({
  selector: 'app-notification-card',
  standalone: true,
  imports: [
    ButtonComponent,
    CardComponent,
    NotificationPriorityBadgeComponent,
    NotificationTypeIconComponent,
  ],
  templateUrl: './notification-card.html',
  styleUrl: './notification-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationCardComponent {
  readonly notification = input.required<Notification>();
  readonly markingRead = input(false);
  readonly showReferenceAction = input(true);

  readonly markAsRead = output<string>();
  readonly referenceSelected = output<Notification>();

  protected readonly isUnread = computed(() => !this.notification().isRead);
  protected readonly isCritical = computed(
    () => this.notification().priority === NotificationPriority.Critical,
  );
  protected readonly typeLabel = computed(() =>
    friendlyNotificationType(this.notification().notificationType),
  );
  protected readonly statusLabel = computed(() =>
    this.notification().isRead ? 'Read notification' : 'Unread notification',
  );

  protected readonly cardClasses = computed(() =>
    [
      'notification-card',
      this.isUnread() ? 'notification-card--unread' : '',
      this.isCritical() ? 'notification-card--critical' : '',
    ]
      .filter(Boolean)
      .join(' '),
  );

  protected referenceLabel(notification: Notification): string {
    if (!notification.referenceType && !notification.referenceId) {
      return 'No linked item';
    }

    return [notification.referenceType, notification.referenceId].filter(Boolean).join(' ');
  }
}
