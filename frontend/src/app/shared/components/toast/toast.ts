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
        ? 'bg-brand-50 text-brand-700'
        : tone === 'error'
          ? 'bg-red-50 text-red-700'
          : 'bg-blue-50 text-blue-700';
    return `grid h-8 w-8 shrink-0 place-items-center rounded-full ${toneClass}`;
  }
}
