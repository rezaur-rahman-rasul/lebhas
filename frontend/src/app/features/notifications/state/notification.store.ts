import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  Notification,
  NotificationActionResult,
  NotificationFilters,
  NotificationPreference,
  NotificationPreferenceUpdateRequest,
  NotificationPriority,
  NotificationType,
} from '../models/notification.models';
import { NotificationApiService } from '../services/notification-api.service';

@Injectable({ providedIn: 'root' })
export class NotificationStore {
  private readonly api = inject(NotificationApiService);
  private readonly permissions = inject(PermissionStore);
  private readonly notificationsState = inject(NotificationStateService);

  private readonly notificationsSignal = signal<readonly Notification[]>([]);
  private readonly unreadCountSignal = signal(0);
  private readonly preferencesSignal = signal<readonly NotificationPreference[]>([]);
  private readonly selectedTypeSignal = signal<NotificationType | string | null>(null);
  private readonly selectedPrioritySignal = signal<NotificationPriority | string | null>(null);
  private readonly unreadOnlySignal = signal(false);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly notifications = this.notificationsSignal.asReadonly();
  readonly unreadCount = this.unreadCountSignal.asReadonly();
  readonly preferences = this.preferencesSignal.asReadonly();
  readonly selectedType = this.selectedTypeSignal.asReadonly();
  readonly selectedPriority = this.selectedPrioritySignal.asReadonly();
  readonly unreadOnly = this.unreadOnlySignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly unreadNotifications = computed(() =>
    this.notificationsSignal().filter((notification) => !notification.isRead),
  );
  readonly criticalNotifications = computed(() =>
    this.notificationsSignal().filter(
      (notification) => notification.priority === NotificationPriority.Critical,
    ),
  );

  async loadNotifications(workspaceId: string): Promise<NotificationActionResult> {
    if (!this.permissions.canViewNotifications()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.notificationsSignal.set(await this.api.getNotifications(workspaceId, this.filters()));
    });
  }

  async loadUnreadCount(workspaceId: string): Promise<NotificationActionResult> {
    if (!this.permissions.canViewNotifications()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.unreadCountSignal.set(await this.api.getUnreadCount(workspaceId));
    });
  }

  async markNotificationAsRead(
    workspaceId: string,
    notificationId: string,
  ): Promise<NotificationActionResult> {
    if (!this.permissions.canViewNotifications()) {
      return this.restricted();
    }

    return this.run(async () => {
      const notification = await this.api.markAsRead(workspaceId, notificationId);
      this.notificationsSignal.update((items) => upsert(items, notification));
      this.unreadCountSignal.update((count) => Math.max(0, count - 1));
    });
  }

  async markAllNotificationsAsRead(workspaceId: string): Promise<NotificationActionResult> {
    if (!this.permissions.canViewNotifications()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.notificationsSignal.set(await this.api.markAllAsRead(workspaceId));
      this.unreadCountSignal.set(0);
    });
  }

  async loadNotificationPreferences(workspaceId: string): Promise<NotificationActionResult> {
    if (!this.permissions.canManageNotificationPreferences()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.preferencesSignal.set(await this.api.getPreferences(workspaceId));
    });
  }

  async updateNotificationPreferences(
    workspaceId: string,
    payload: readonly NotificationPreferenceUpdateRequest[],
  ): Promise<NotificationActionResult> {
    if (!this.permissions.canManageNotificationPreferences()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.preferencesSignal.set(await this.api.updatePreferences(workspaceId, payload));
    });
  }

  setSelectedNotificationType(type: NotificationType | string | null): void {
    this.selectedTypeSignal.set(type);
  }

  setSelectedPriority(priority: NotificationPriority | string | null): void {
    this.selectedPrioritySignal.set(priority);
  }

  setUnreadOnly(value: boolean): void {
    this.unreadOnlySignal.set(value);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  private filters(): NotificationFilters {
    return {
      notificationType: this.selectedTypeSignal(),
      priority: this.selectedPrioritySignal(),
      unreadOnly: this.unreadOnlySignal(),
    };
  }

  private async run(action: () => Promise<void>): Promise<NotificationActionResult> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      await action();
      return { ok: true };
    } catch (error) {
      const message = friendlyError(error, 'We could not load notifications. Please try again.');
      this.errorSignal.set(message);
      this.notificationsState.error('Notifications', message);
      return { ok: false, message };
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private restricted(): NotificationActionResult {
    const message = 'You do not have access to notifications.';
    this.errorSignal.set(message);
    return { ok: false, message };
  }
}

function upsert<T extends { readonly id: string }>(items: readonly T[], item: T): readonly T[] {
  return items.some((current) => current.id === item.id)
    ? items.map((current) => (current.id === item.id ? item : current))
    : [...items, item];
}

function friendlyError(error: unknown, fallback: string): string {
  return normalizeHttpError(error).message || fallback;
}
