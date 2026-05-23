import { Injectable, signal } from '@angular/core';

export type NotificationTone = 'success' | 'error' | 'info' | 'warning';

export interface AppNotification {
  readonly id: string;
  readonly tone: NotificationTone;
  readonly title: string;
  readonly message?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationStateService {
  private readonly notificationsSignal = signal<readonly AppNotification[]>([]);

  readonly notifications = this.notificationsSignal.asReadonly();

  success(title: string, message?: string): void {
    this.push({ tone: 'success', title, message });
  }

  error(title: string, message?: string): void {
    this.push({ tone: 'error', title, message });
  }

  info(title: string, message?: string): void {
    this.push({ tone: 'info', title, message });
  }

  dismiss(id: string): void {
    this.notificationsSignal.update((items) => items.filter((item) => item.id !== id));
  }

  private push(notification: Omit<AppNotification, 'id'>): void {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    this.notificationsSignal.update((items) => [...items, { id, ...notification }].slice(-4));
    window.setTimeout(() => this.dismiss(id), 5000);
  }
}
