import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiEndpoints } from '@app/core/api/api-endpoints';
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
      ApiEndpoints.notifications.list(workspaceId),
      filters,
    );
  }

  getUnreadCount(workspaceId: string): Promise<number> {
    return this.get<number>(ApiEndpoints.notifications.unreadCount(workspaceId));
  }

  markAsRead(workspaceId: string, notificationId: string): Promise<Notification> {
    return this.post<Notification, Record<string, never>>(
      ApiEndpoints.notifications.read(workspaceId, notificationId),
      {},
    );
  }

  markAllAsRead(workspaceId: string): Promise<readonly Notification[]> {
    return this.post<readonly Notification[], Record<string, never>>(
      ApiEndpoints.notifications.readAll(workspaceId),
      {},
    );
  }

  getPreferences(workspaceId: string): Promise<readonly NotificationPreference[]> {
    return this.get<readonly NotificationPreference[]>(
      ApiEndpoints.notifications.preferences(workspaceId),
    );
  }

  updatePreferences(
    workspaceId: string,
    payload: readonly NotificationPreferenceUpdateRequest[],
  ): Promise<readonly NotificationPreference[]> {
    return this.put<readonly NotificationPreference[], readonly NotificationPreferenceUpdateRequest[]>(
      ApiEndpoints.notifications.preferences(workspaceId),
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
}
