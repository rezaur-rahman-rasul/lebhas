import { Injectable, signal } from '@angular/core';
import { timer } from 'rxjs';

export type NotificationTone = 'success' | 'error' | 'info' | 'warning';

export interface AppNotification {
  readonly id: string;
  readonly tone: NotificationTone;
  readonly title: string;
  readonly message?: string;
  readonly key: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationStateService {
  private readonly duplicateWindowMs = 3000;
  private readonly visibleLimit = 2;
  private readonly lastShownByKey = new Map<string, number>();
  private readonly notificationsSignal = signal<readonly AppNotification[]>([]);

  readonly notifications = this.notificationsSignal.asReadonly();

  success(title: string, message?: string, key?: string): void {
    this.push({ tone: 'success', title, message, key });
  }

  error(title: string, message?: string, key?: string): void {
    this.push({ tone: 'error', title, message, key });
  }

  info(title: string, message?: string, key?: string): void {
    this.push({ tone: 'info', title, message, key });
  }

  dismiss(id: string): void {
    this.notificationsSignal.update((items) => items.filter((item) => item.id !== id));
  }

  private push(notification: Omit<AppNotification, 'id' | 'key'> & { readonly key?: string }): void {
    const key = notification.key ?? this.notificationKey(notification);
    const now = Date.now();
    const lastShownAt = this.lastShownByKey.get(key) ?? 0;

    if (now - lastShownAt < this.duplicateWindowMs) {
      return;
    }

    this.lastShownByKey.set(key, now);
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    this.notificationsSignal.update((items) =>
      [...items.filter((item) => item.key !== key), { id, ...notification, key }].slice(-this.visibleLimit),
    );
    timer(5000).subscribe(() => this.dismiss(id));
  }

  private notificationKey(notification: Omit<AppNotification, 'id' | 'key'>): string {
    return [notification.tone, notification.title, notification.message ?? '']
      .join('|')
      .toLowerCase();
  }
}
