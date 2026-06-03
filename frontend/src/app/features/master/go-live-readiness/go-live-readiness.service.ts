import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiEndpoints } from '@app/core/api/api-endpoints';
import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';

export type ReadinessStatus = 'READY' | 'NEEDS_ATTENTION' | 'BLOCKED';
export type ReadinessSeverity = 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';

export interface GoLiveReadinessCheck {
  readonly key: string;
  readonly title: string;
  readonly description: string;
  readonly status: ReadinessStatus;
  readonly severity: ReadinessSeverity | string;
  readonly relatedRoute: string;
  readonly lastCheckedAt: string;
}

export interface GoLiveReadinessResponse {
  readonly ready: number;
  readonly needsAttention: number;
  readonly blocked: number;
  readonly checks: readonly GoLiveReadinessCheck[];
}

@Injectable({ providedIn: 'root' })
export class GoLiveReadinessApiService {
  private readonly api = inject(ApiService);

  async getReadiness(): Promise<GoLiveReadinessResponse> {
    const response = await firstValueFrom(
      this.api.get<GoLiveReadinessResponse>(ApiEndpoints.master.goLiveReadiness),
    );
    const data = unwrapApiResponse(response);
    return {
      ready: data?.ready ?? 0,
      needsAttention: data?.needsAttention ?? 0,
      blocked: data?.blocked ?? 0,
      checks: data?.checks ?? [],
    };
  }
}
