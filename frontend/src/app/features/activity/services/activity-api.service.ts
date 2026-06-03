import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiEndpoints } from '@app/core/api/api-endpoints';
import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import { ActivityFeed, ActivityFilters } from '../models/activity.models';

type FilterValue = string | number | boolean;
type FilterParams = Record<string, FilterValue>;

@Injectable({ providedIn: 'root' })
export class ActivityApiService {
  private readonly api = inject(ApiService);

  getActivityFeed(workspaceId: string, filters?: ActivityFilters): Promise<readonly ActivityFeed[]> {
    return this.get<readonly ActivityFeed[]>(
      ApiEndpoints.activity.feed(workspaceId),
      filters,
    );
  }

  async getActivityDetail(workspaceId: string, activityId: string): Promise<ActivityFeed> {
    const items = await this.getActivityFeed(workspaceId);
    const item = items.find((activity) => activity.id === activityId);
    if (!item) {
      throw new Error('Activity item not found in the workspace feed.');
    }
    return item;
  }

  private async get<T>(path: string, filters?: ActivityFilters): Promise<T> {
    const response = await firstValueFrom(this.api.get<T>(path, this.filters(filters)));
    return unwrapApiResponse(response);
  }

  private filters(filters?: ActivityFilters): FilterParams {
    const params: FilterParams = {};
    for (const [key, value] of Object.entries(filters ?? {})) {
      if (value !== null && value !== undefined && value !== '') {
        params[key] = value;
      }
    }
    return params;
  }
}
