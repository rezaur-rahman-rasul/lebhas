import { HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  ConfigureLayerRoutingPolicyPayload,
  CreativePipelineLayerView,
  CreativePipelineView,
  CreativeToolView,
  ProviderRoutingPolicyView,
} from './creative-pipeline.models';

@Injectable({ providedIn: 'root' })
export class CreativePipelineService {
  private readonly api = inject(ApiService);
  private readonly quietContext = new HttpContext().set(SKIP_ERROR_TOAST, true);

  async listPipelines(): Promise<readonly CreativePipelineView[]> {
    const response = await firstValueFrom(
      this.api.get<CreativePipelineView[]>('/api/v1/master/creative-pipelines', { context: this.quietContext }),
    );

    return unwrapApiResponse(response);
  }

  async listTools(): Promise<readonly CreativeToolView[]> {
    const response = await firstValueFrom(
      this.api.get<readonly CreativeToolView[]>('/api/v1/master/creative-tools', { context: this.quietContext }),
    );

    return unwrapApiResponse(response);
  }

  async listLayers(): Promise<readonly CreativePipelineLayerView[]> {
    const response = await firstValueFrom(
      this.api.get<readonly CreativePipelineLayerView[]>('/api/v1/master/creative-layers', { context: this.quietContext }),
    );

    return unwrapApiResponse(response);
  }

  async listRoutingPolicies(): Promise<readonly ProviderRoutingPolicyView[]> {
    const response = await firstValueFrom(
      this.api.get<readonly ProviderRoutingPolicyView[]>('/api/v1/master/provider-routing-policies', { context: this.quietContext }),
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
