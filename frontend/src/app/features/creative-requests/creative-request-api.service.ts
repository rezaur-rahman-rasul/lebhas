import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiEndpoints } from '@app/core/api/api-endpoints';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  CreateCreativeRequestPayload,
  CreativeRequest,
  UpdateCreativeRequestPayload,
} from './creative-request.models';

@Injectable({ providedIn: 'root' })
export class CreativeRequestApiService {
  private readonly api = inject(ApiService);

  async listByProject(workspaceId: string, projectId: string): Promise<readonly CreativeRequest[]> {
    const response = await firstValueFrom(
      this.api.get<readonly CreativeRequest[]>(
        ApiEndpoints.creativeRequests.listByProject(workspaceId, projectId),
      ),
    );
    return unwrapApiResponse(response);
  }

  async get(workspaceId: string, creativeRequestId: string): Promise<CreativeRequest> {
    const response = await firstValueFrom(
      this.api.get<CreativeRequest>(
        ApiEndpoints.creativeRequests.detail(workspaceId, creativeRequestId),
      ),
    );
    return unwrapApiResponse(response);
  }

  async create(
    workspaceId: string,
    projectId: string,
    payload: CreateCreativeRequestPayload,
  ): Promise<CreativeRequest> {
    const response = await firstValueFrom(
      this.api.post<CreativeRequest, CreateCreativeRequestPayload>(
        ApiEndpoints.creativeRequests.listByProject(workspaceId, projectId),
        payload,
      ),
    );
    return unwrapApiResponse(response);
  }

  async update(
    workspaceId: string,
    creativeRequestId: string,
    payload: UpdateCreativeRequestPayload,
  ): Promise<CreativeRequest> {
    const response = await firstValueFrom(
      this.api.put<CreativeRequest, UpdateCreativeRequestPayload>(
        ApiEndpoints.creativeRequests.detail(workspaceId, creativeRequestId),
        payload,
      ),
    );
    return unwrapApiResponse(response);
  }
}
