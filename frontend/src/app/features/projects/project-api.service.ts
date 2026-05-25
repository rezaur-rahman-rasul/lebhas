import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  CreateProjectCampaignPayload,
  ProjectCampaign,
  UpdateProjectCampaignPayload,
} from './project.models';

@Injectable({ providedIn: 'root' })
export class ProjectApiService {
  private readonly api = inject(ApiService);

  async list(workspaceId: string): Promise<readonly ProjectCampaign[]> {
    const response = await firstValueFrom(
      this.api.get<ProjectCampaign[]>(`/api/v1/workspaces/${workspaceId}/projects`),
    );

    return unwrapApiResponse(response);
  }

  async create(
    workspaceId: string,
    productServiceId: string,
    payload: CreateProjectCampaignPayload,
  ): Promise<ProjectCampaign> {
    const response = await firstValueFrom(
      this.api.post<ProjectCampaign, CreateProjectCampaignPayload>(
        `/api/v1/workspaces/${workspaceId}/product-services/${productServiceId}/projects`,
        payload,
      ),
    );

    return unwrapApiResponse(response);
  }

  async update(
    workspaceId: string,
    projectId: string,
    payload: UpdateProjectCampaignPayload,
  ): Promise<ProjectCampaign> {
    const response = await firstValueFrom(
      this.api.put<ProjectCampaign, UpdateProjectCampaignPayload>(
        `/api/v1/workspaces/${workspaceId}/projects/${projectId}`,
        payload,
      ),
    );

    return unwrapApiResponse(response);
  }

  async remove(workspaceId: string, projectId: string): Promise<void> {
    await firstValueFrom(this.api.delete<void>(`/api/v1/workspaces/${workspaceId}/projects/${projectId}`));
  }
}
