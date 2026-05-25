import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
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
        `/api/v1/workspaces/${workspaceId}/projects/${projectId}/creative-requests`,
      ),
    );
    return unwrapApiResponse(response);
  }

  async get(workspaceId: string, creativeRequestId: string): Promise<CreativeRequest> {
    const response = await firstValueFrom(
      this.api.get<CreativeRequest>(
        `/api/v1/workspaces/${workspaceId}/creative-requests/${creativeRequestId}`,
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
        `/api/v1/workspaces/${workspaceId}/projects/${projectId}/creative-requests`,
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
        `/api/v1/workspaces/${workspaceId}/creative-requests/${creativeRequestId}`,
        payload,
      ),
    );
    return unwrapApiResponse(response);
  }
}
