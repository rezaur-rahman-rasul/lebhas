import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiEndpoints } from '@app/core/api/api-endpoints';
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
        ApiEndpoints.creativeRequests.generatedVersions(workspaceId, creativeRequestId),
      ),
    );
    return unwrapApiResponse(response);
  }

  async get(workspaceId: string, generatedVersionId: string): Promise<GeneratedVersion> {
    const response = await firstValueFrom(
      this.api.get<GeneratedVersion>(
        ApiEndpoints.generatedVersions.detail(workspaceId, generatedVersionId),
      ),
    );
    return unwrapApiResponse(response);
  }

  async getDownloadUrl(workspaceId: string, generatedVersionId: string): Promise<GeneratedVersionLink> {
    const response = await firstValueFrom(
      this.api.post<GeneratedVersionLink, Record<string, never>>(
        ApiEndpoints.generatedVersions.downloadUrl(workspaceId, generatedVersionId),
        {},
      ),
    );
    return unwrapApiResponse(response);
  }

  async getShareUrl(workspaceId: string, generatedVersionId: string): Promise<GeneratedVersionLink> {
    const response = await firstValueFrom(
      this.api.post<GeneratedVersionLink, Record<string, never>>(
        ApiEndpoints.generatedVersions.shareLinks(workspaceId, generatedVersionId),
        {},
      ),
    );
    return unwrapApiResponse(response);
  }
}
