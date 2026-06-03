import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiEndpoints } from '@app/core/api/api-endpoints';
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
      ApiEndpoints.activity.auditLogs(workspaceId),
      filters,
    );
  }

  getMasterAuditLogs(filters?: AuditLogFilters): Promise<readonly AuditLog[]> {
    return this.getList<AuditLog>(ApiEndpoints.master.auditLogs, filters);
  }

  private async get<T>(path: string, filters?: AuditLogFilters): Promise<T> {
    const response = await firstValueFrom(this.api.get<T>(path, this.filters(filters)));
    return unwrapApiResponse(response);
  }

  private async getList<T>(path: string, filters?: AuditLogFilters): Promise<readonly T[]> {
    const data = await this.get<readonly T[] | { readonly items?: readonly T[] } | null>(path, filters);
    if (Array.isArray(data)) {
      return data;
    }
    const pagedData = data as { readonly items?: readonly T[] } | null;
    return pagedData?.items ?? [];
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
}
