import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { NotificationStateService } from '@app/core/state/notification-state.service';
import { IconComponent } from '../icon/icon';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './toast.html',
  styleUrl: './toast.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastComponent {
  protected readonly notifications = inject(NotificationStateService);

  protected iconName(tone: string): string {
    return tone === 'success' ? 'circle-check' : tone === 'error' ? 'circle-alert' : 'info';
  }

  protected iconWrap(tone: string): string {
    const toneClass =
      tone === 'success'
        ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-100'
        : tone === 'error'
          ? 'bg-red-50 text-red-700 dark:bg-red-500/15 dark:text-red-100'
          : 'bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-100';
    return `grid h-8 w-8 shrink-0 place-items-center rounded-full ${toneClass}`;
  }
}
