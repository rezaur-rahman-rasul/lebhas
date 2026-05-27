import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import { AuditLog, AuditLogFilters } from '../models/audit.models';

type FilterValue = string | number | boolean;
type FilterParams = Record<string, FilterValue>;

@Injectable({ providedIn: 'root' })
export class AuditApiService {
  private readonly api = inject(ApiService);

  getAuditLogs(workspaceId: string, filters?: AuditLogFilters): Promise<readonly AuditLog[]> {
    return this.get<readonly AuditLog[]>(
      `/api/v1/workspaces/${this.path(workspaceId)}/audit-logs`,
      filters,
    );
  }

  private async get<T>(path: string, filters?: AuditLogFilters): Promise<T> {
    const response = await firstValueFrom(this.api.get<T>(path, this.filters(filters)));
    return unwrapApiResponse(response);
  }

  private filters(filters?: AuditLogFilters): FilterParams {
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
