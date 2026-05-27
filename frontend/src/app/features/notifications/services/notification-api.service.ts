import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  Notification,
  NotificationFilters,
  NotificationPreference,
  NotificationPreferenceUpdateRequest,
} from '../models/notification.models';

type FilterValue = string | number | boolean;
type FilterParams = Record<string, FilterValue>;

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private readonly api = inject(ApiService);

  getNotifications(
    workspaceId: string,
    filters?: NotificationFilters,
  ): Promise<readonly Notification[]> {
    return this.get<readonly Notification[]>(
      `/api/v1/workspaces/${this.path(workspaceId)}/notifications`,
      filters,
    );
  }

  getUnreadCount(workspaceId: string): Promise<number> {
    return this.get<number>(`/api/v1/workspaces/${this.path(workspaceId)}/notifications/unread-count`);
  }

  markAsRead(workspaceId: string, notificationId: string): Promise<Notification> {
    return this.post<Notification, Record<string, never>>(
      `/api/v1/workspaces/${this.path(workspaceId)}/notifications/${this.path(notificationId)}/read`,
      {},
    );
  }

  markAllAsRead(workspaceId: string): Promise<readonly Notification[]> {
    return this.post<readonly Notification[], Record<string, never>>(
      `/api/v1/workspaces/${this.path(workspaceId)}/notifications/read-all`,
      {},
    );
  }

  getPreferences(workspaceId: string): Promise<readonly NotificationPreference[]> {
    return this.get<readonly NotificationPreference[]>(
      `/api/v1/workspaces/${this.path(workspaceId)}/notification-preferences`,
    );
  }

  updatePreferences(
    workspaceId: string,
    payload: readonly NotificationPreferenceUpdateRequest[],
  ): Promise<readonly NotificationPreference[]> {
    return this.put<readonly NotificationPreference[], readonly NotificationPreferenceUpdateRequest[]>(
      `/api/v1/workspaces/${this.path(workspaceId)}/notification-preferences`,
      payload,
    );
  }

  private async get<T>(path: string, filters?: NotificationFilters): Promise<T> {
    const response = await firstValueFrom(this.api.get<T>(path, this.filters(filters)));
    return unwrapApiResponse(response);
  }

  private async post<T, TBody>(path: string, body: TBody): Promise<T> {
    const response = await firstValueFrom(this.api.post<T, TBody>(path, body));
    return unwrapApiResponse(response);
  }

  private async put<T, TBody>(path: string, body: TBody): Promise<T> {
    const response = await firstValueFrom(this.api.put<T, TBody>(path, body));
    return unwrapApiResponse(response);
  }

  private filters(filters?: NotificationFilters): FilterParams {
    const params: FilterParams = {};
    for (const [key, value] of Object.entries(filters ?? {})) {
      if (value !== null && value !== undefined && value !== '') {
        params[key] = value;
      }
    }
    return params;
  }

  private path(value: string): string {
    return encodeURIComponent(value);
  }
}
