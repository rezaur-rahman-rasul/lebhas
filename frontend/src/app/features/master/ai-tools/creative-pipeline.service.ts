import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  ConfigureLayerRoutingPolicyPayload,
  CreativePipelineView,
} from './creative-pipeline.models';

@Injectable({ providedIn: 'root' })
export class CreativePipelineService {
  private readonly api = inject(ApiService);

  async listPipelines(): Promise<readonly CreativePipelineView[]> {
    const response = await firstValueFrom(
      this.api.get<CreativePipelineView[]>('/api/v1/master/creative-pipelines'),
    );

    return unwrapApiResponse(response);
  }

  async configureRoutingPolicy(
    pipelineId: string,
    layerId: string,
    payload: ConfigureLayerRoutingPolicyPayload,
  ): Promise<CreativePipelineView> {
    const response = await firstValueFrom(
      this.api.put<CreativePipelineView, ConfigureLayerRoutingPolicyPayload>(
        `/api/v1/master/creative-pipelines/${encodeURIComponent(pipelineId)}/layers/${encodeURIComponent(layerId)}/routing-policy`,
        payload,
      ),
    );

    return unwrapApiResponse(response);
  }
}
