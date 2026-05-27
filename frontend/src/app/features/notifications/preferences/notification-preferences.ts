import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { NotificationEmptyStateComponent } from '../components/notification-empty-state/notification-empty-state';
import { NotificationLoadingStateComponent } from '../components/notification-loading-state/notification-loading-state';
import {
  NotificationTypeIconComponent,
  friendlyNotificationType,
} from '../components/notification-type-icon/notification-type-icon';
import {
  NotificationPreference,
  NotificationPreferenceUpdateRequest,
} from '../models/notification.models';
import { NotificationStore } from '../state/notification.store';

@Component({
  selector: 'app-notification-preferences',
  standalone: true,
  imports: [
    RouterLink,
    ButtonComponent,
    CardComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    NotificationEmptyStateComponent,
    NotificationLoadingStateComponent,
    NotificationTypeIconComponent,
  ],
  templateUrl: './notification-preferences.html',
  styleUrl: './notification-preferences.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationPreferencesPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(NotificationStore);

  protected readonly draft = signal<readonly NotificationPreferenceUpdateRequest[]>([]);
  protected readonly savedMessage = signal<string | null>(null);

  protected readonly activeWorkspaceId = this.workspace.activeWorkspaceId;
  protected readonly accessDenied = computed(
    () => !this.permissions.canManageNotificationPreferences(),
  );
  protected readonly hasPreferences = computed(() => this.draft().length > 0);

  constructor() {
    effect(() => {
      const workspaceId = this.activeWorkspaceId();
      if (!workspaceId || this.accessDenied()) {
        return;
      }

      void this.loadPreferences(workspaceId);
    });
  }

  protected refresh(): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId || this.accessDenied()) {
      return;
    }

    void this.loadPreferences(workspaceId);
  }

  protected updateToggle(
    notificationType: string,
    field: keyof Omit<NotificationPreferenceUpdateRequest, 'notificationType'>,
    value: boolean,
  ): void {
    this.savedMessage.set(null);
    this.draft.update((preferences) =>
      preferences.map((preference) =>
        preference.notificationType === notificationType
          ? { ...preference, [field]: value }
          : preference,
      ),
    );
  }

  protected save(): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId || this.accessDenied()) {
      return;
    }

    void this.savePreferences(workspaceId);
  }

  protected clearError(): void {
    this.store.clearError();
  }

  protected friendlyType(notificationType: string): string {
    return friendlyNotificationType(notificationType);
  }

  private async loadPreferences(workspaceId: string): Promise<void> {
    const result = await this.store.loadNotificationPreferences(workspaceId);
    if (result.ok) {
      this.draft.set(this.store.preferences().map(toUpdateRequest));
    }
  }

  private async savePreferences(workspaceId: string): Promise<void> {
    const result = await this.store.updateNotificationPreferences(workspaceId, this.draft());
    if (result.ok) {
      this.draft.set(this.store.preferences().map(toUpdateRequest));
      this.savedMessage.set('Notification preferences saved.');
    }
  }
}

function toUpdateRequest(preference: NotificationPreference): NotificationPreferenceUpdateRequest {
  return {
    notificationType: preference.notificationType,
    inAppEnabled: preference.inAppEnabled,
    emailEnabled: preference.emailEnabled,
    smsEnabled: preference.smsEnabled,
    pushEnabled: preference.pushEnabled,
  };
}
