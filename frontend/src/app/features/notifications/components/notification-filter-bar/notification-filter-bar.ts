import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { NotificationPriority, NotificationType } from '../../models/notification.models';
import { friendlyNotificationType } from '../notification-type-icon/notification-type-icon';

@Component({
  selector: 'app-notification-filter-bar',
  standalone: true,
  imports: [ButtonComponent],
  templateUrl: './notification-filter-bar.html',
  styleUrl: './notification-filter-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationFilterBarComponent {
  readonly selectedType = input<NotificationType | string | null>(null);
  readonly selectedPriority = input<NotificationPriority | string | null>(null);
  readonly unreadOnly = input(false);
  readonly selectedDate = input<string | null>(null);
  readonly loading = input(false);

  readonly selectedTypeChange = output<NotificationType | string | null>();
  readonly selectedPriorityChange = output<NotificationPriority | string | null>();
  readonly unreadOnlyChange = output<boolean>();
  readonly selectedDateChange = output<string | null>();
  readonly refresh = output<void>();
  readonly clear = output<void>();

  protected readonly notificationTypes = Object.values(NotificationType);
  protected readonly priorities = Object.values(NotificationPriority);

  protected updateType(value: string): void {
    this.selectedTypeChange.emit(value || null);
  }

  protected updatePriority(value: string): void {
    this.selectedPriorityChange.emit(value || null);
  }

  protected updateDate(value: string): void {
    this.selectedDateChange.emit(value || null);
  }

  protected typeLabel(value: string): string {
    return friendlyNotificationType(value);
  }

  protected priorityLabel(value: string): string {
    switch (value) {
      case NotificationPriority.High:
        return 'High priority';
      case NotificationPriority.Critical:
        return 'Critical attention';
      default:
        return value.toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
    }
  }
}
