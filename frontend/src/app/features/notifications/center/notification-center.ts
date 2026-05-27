import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { NotificationCardComponent } from '../components/notification-card/notification-card';
import { NotificationEmptyStateComponent } from '../components/notification-empty-state/notification-empty-state';
import { NotificationFilterBarComponent } from '../components/notification-filter-bar/notification-filter-bar';
import { NotificationLoadingStateComponent } from '../components/notification-loading-state/notification-loading-state';
import { NotificationTypeIconComponent } from '../components/notification-type-icon/notification-type-icon';
import { UnreadCountPillComponent } from '../components/unread-count-pill/unread-count-pill';
import { Notification, NotificationPriority, NotificationType } from '../models/notification.models';
import { NotificationStore } from '../state/notification.store';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [
    RouterLink,
    ButtonComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    NotificationCardComponent,
    NotificationEmptyStateComponent,
    NotificationFilterBarComponent,
    NotificationLoadingStateComponent,
    NotificationTypeIconComponent,
    UnreadCountPillComponent,
  ],
  templateUrl: './notification-center.html',
  styleUrl: './notification-center.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationCenterPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(NotificationStore);

  protected readonly selectedDate = signal<string | null>(null);
  protected readonly referenceMessage = signal<string | null>(null);

  protected readonly activeWorkspaceId = this.workspace.activeWorkspaceId;
  protected readonly accessDenied = computed(() => !this.permissions.canViewNotifications());
  protected readonly hasNotifications = computed(() => this.store.notifications().length > 0);
  protected readonly criticalCount = computed(() => this.store.criticalNotifications().length);
  protected readonly unreadCount = this.store.unreadCount;

  constructor() {
    effect(() => {
      const workspaceId = this.activeWorkspaceId();
      if (!workspaceId || !this.permissions.canViewNotifications()) {
        return;
      }

      void this.loadNotifications(workspaceId);
    });
  }

  protected refresh(): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId || this.accessDenied()) {
      return;
    }

    void this.loadNotifications(workspaceId);
  }

  protected markAsRead(notificationId: string): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId) {
      return;
    }

    void this.store.markNotificationAsRead(workspaceId, notificationId);
  }

  protected markAllAsRead(): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId || this.unreadCount() === 0) {
      return;
    }

    void this.store.markAllNotificationsAsRead(workspaceId);
  }

  protected updateType(type: NotificationType | string | null): void {
    this.store.setSelectedNotificationType(type);
    this.refresh();
  }

  protected updatePriority(priority: NotificationPriority | string | null): void {
    this.store.setSelectedPriority(priority);
    this.refresh();
  }

  protected updateUnreadOnly(value: boolean): void {
    this.store.setUnreadOnly(value);
    this.refresh();
  }

  protected updateDate(date: string | null): void {
    this.selectedDate.set(date);
  }

  protected clearFilters(): void {
    this.store.setSelectedNotificationType(null);
    this.store.setSelectedPriority(null);
    this.store.setUnreadOnly(false);
    this.selectedDate.set(null);
    this.refresh();
  }

  protected openReference(notification: Notification): void {
    const label = [notification.referenceType, notification.referenceId].filter(Boolean).join(' ');
    this.referenceMessage.set(label ? `Linked item selected: ${label}` : null);
  }

  protected clearError(): void {
    this.store.clearError();
  }

  private async loadNotifications(workspaceId: string): Promise<void> {
    await Promise.all([
      this.store.loadNotifications(workspaceId),
      this.store.loadUnreadCount(workspaceId),
    ]);
  }
}
