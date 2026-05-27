import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { NotificationPriority } from '../../models/notification.models';

@Component({
  selector: 'app-notification-priority-badge',
  standalone: true,
  templateUrl: './notification-priority-badge.html',
  styleUrl: './notification-priority-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationPriorityBadgeComponent {
  readonly priority = input<NotificationPriority | string>(NotificationPriority.Normal);

  protected readonly label = computed(() => {
    switch (this.priority()) {
      case NotificationPriority.Low:
        return 'Low';
      case NotificationPriority.High:
        return 'High priority';
      case NotificationPriority.Critical:
        return 'Critical attention';
      default:
        return 'Normal';
    }
  });

  protected readonly classes = computed(() => {
    const base = 'inline-flex max-w-full items-center rounded-full border px-2.5 py-1 text-xs font-medium';

    switch (this.priority()) {
      case NotificationPriority.Low:
        return `${base} border-border bg-panel text-muted`;
      case NotificationPriority.High:
        return `${base} border-warning-500/30 bg-warning-500/10 text-warning-700 dark:text-warning-300`;
      case NotificationPriority.Critical:
        return `${base} border-alert-500/30 bg-alert-500/10 text-alert-700 dark:text-alert-300`;
      default:
        return `${base} border-brand-500/25 bg-brand-500/10 text-brand-700 dark:text-brand-300`;
    }
  });
}
