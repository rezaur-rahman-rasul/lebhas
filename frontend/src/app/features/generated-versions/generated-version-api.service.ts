import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import { GeneratedVersion, GeneratedVersionLink } from './generated-version.models';

@Injectable({ providedIn: 'root' })
export class GeneratedVersionApiService {
  private readonly api = inject(ApiService);

  async listByCreativeRequest(
    workspaceId: string,
    creativeRequestId: string,
  ): Promise<readonly GeneratedVersion[]> {
    const response = await firstValueFrom(
      this.api.get<readonly GeneratedVersion[]>(
        `/api/v1/workspaces/${workspaceId}/creative-requests/${creativeRequestId}/generated-versions`,
      ),
    );
    return unwrapApiResponse(response);
  }

  async get(workspaceId: string, generatedVersionId: string): Promise<GeneratedVersion> {
    const response = await firstValueFrom(
      this.api.get<GeneratedVersion>(
        `/api/v1/workspaces/${workspaceId}/generated-versions/${generatedVersionId}`,
      ),
    );
    return unwrapApiResponse(response);
  }

  async getDownloadUrl(workspaceId: string, generatedVersionId: string): Promise<GeneratedVersionLink> {
    const response = await firstValueFrom(
      this.api.get<GeneratedVersionLink>(
        `/api/v1/workspaces/${workspaceId}/generated-versions/${generatedVersionId}/download-url`,
      ),
    );
    return unwrapApiResponse(response);
  }

  async getShareUrl(workspaceId: string, generatedVersionId: string): Promise<GeneratedVersionLink> {
    const response = await firstValueFrom(
      this.api.get<GeneratedVersionLink>(
        `/api/v1/workspaces/${workspaceId}/generated-versions/${generatedVersionId}/share-url`,
      ),
    );
    return unwrapApiResponse(response);
  }
}
