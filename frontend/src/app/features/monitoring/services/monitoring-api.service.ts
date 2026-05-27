import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  AiProviderMonitoringSummary,
  MonitoringAlert,
  MonitoringAlertFilters,
  PaymentMonitoringSummary,
  SystemHealthEvent,
  WorkspaceMonitoringSummary,
} from '../models/monitoring.models';

type FilterValue = string | number | boolean;
type FilterParams = Record<string, FilterValue>;

@Injectable({ providedIn: 'root' })
export class MonitoringApiService {
  private readonly api = inject(ApiService);

  getSystemHealth(): Promise<readonly SystemHealthEvent[]> {
    return this.get<readonly SystemHealthEvent[]>('/api/v1/master/monitoring/system-health');
  }

  getAlerts(filters?: MonitoringAlertFilters): Promise<readonly MonitoringAlert[]> {
    return this.get<readonly MonitoringAlert[]>('/api/v1/master/monitoring/alerts', filters);
  }

  getAiProviderMonitoring(): Promise<AiProviderMonitoringSummary> {
    return this.get<AiProviderMonitoringSummary>('/api/v1/master/monitoring/ai-providers');
  }

  getPaymentMonitoring(): Promise<PaymentMonitoringSummary> {
    return this.get<PaymentMonitoringSummary>('/api/v1/master/monitoring/payments');
  }

  getWorkspaceMonitoring(): Promise<WorkspaceMonitoringSummary> {
    return this.get<WorkspaceMonitoringSummary>('/api/v1/master/monitoring/workspaces');
  }

  private async get<T>(path: string, filters?: MonitoringAlertFilters): Promise<T> {
    const response = await firstValueFrom(this.api.get<T>(path, this.filters(filters)));
    return unwrapApiResponse(response);
  }

  private filters(filters?: MonitoringAlertFilters): FilterParams {
    const params: FilterParams = {};
    for (const [key, value] of Object.entries(filters ?? {})) {
      if (value !== null && value !== undefined && value !== '') {
        params[key] = value;
      }
    }
    return params;
  }
}
