import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiEndpoints } from '@app/core/api/api-endpoints';
import { GeneratedVersion, GeneratedVersionLink } from './generated-version.models';

export interface GeneratedVersionListFilters {
  readonly status?: string | null;
  readonly creativeType?: string | null;
  readonly platform?: string | null;
  readonly search?: string | null;
}

@Injectable({ providedIn: 'root' })
export class GeneratedVersionApiService {
  private readonly api = inject(ApiService);

  async list(
    workspaceId: string,
    filters: GeneratedVersionListFilters = {},
  ): Promise<readonly GeneratedVersion[]> {
    const response = await firstValueFrom(
      this.api.get<unknown>(ApiEndpoints.generatedVersions.list(workspaceId), {
        params: {
          status: filters.status,
          creativeType: filters.creativeType,
          platform: filters.platform,
          search: filters.search,
        },
      }),
    );
    return extractVersions(response);
  }

  async listByCreativeRequest(
    workspaceId: string,
    creativeRequestId: string,
  ): Promise<readonly GeneratedVersion[]> {
    const response = await firstValueFrom(
      this.api.get<unknown>(
        ApiEndpoints.creativeRequests.generatedVersions(workspaceId, creativeRequestId),
      ),
    );
    return extractVersions(response);
  }

  async get(workspaceId: string, generatedVersionId: string): Promise<GeneratedVersion> {
    const response = await firstValueFrom(
      this.api.get<GeneratedVersion>(
        ApiEndpoints.generatedVersions.detail(workspaceId, generatedVersionId),
      ),
    );
    return extractOne<GeneratedVersion>(response);
  }

  async getDownloadUrl(workspaceId: string, generatedVersionId: string): Promise<GeneratedVersionLink> {
    const response = await firstValueFrom(
      this.api.post<GeneratedVersionLink, Record<string, never>>(
        ApiEndpoints.generatedVersions.downloadUrl(workspaceId, generatedVersionId),
        {},
      ),
    );
    return extractOne<GeneratedVersionLink>(response);
  }

  async getShareUrl(workspaceId: string, generatedVersionId: string): Promise<GeneratedVersionLink> {
    const response = await firstValueFrom(
      this.api.post<GeneratedVersionLink, Record<string, never>>(
        ApiEndpoints.generatedVersions.shareLinks(workspaceId, generatedVersionId),
        {},
      ),
    );
    return extractOne<GeneratedVersionLink>(response);
  }
}

function extractVersions(response: unknown): readonly GeneratedVersion[] {
  const value = unwrapFlexible(response);
  if (Array.isArray(value)) {
    return value as readonly GeneratedVersion[];
  }
  if (isRecord(value)) {
    for (const key of ['content', 'items', 'versions', 'generatedVersions', 'data']) {
      const nested = value[key];
      if (Array.isArray(nested)) {
        return nested as readonly GeneratedVersion[];
      }
      if (isRecord(nested)) {
        const nestedArray = extractVersions(nested);
        if (nestedArray.length > 0) {
          return nestedArray;
        }
      }
    }
  }
  return [];
}

function extractOne<T>(response: unknown): T {
  return unwrapFlexible(response) as T;
}

function unwrapFlexible(response: unknown): unknown {
  let value = response;
  while (isRecord(value) && 'data' in value) {
    value = value['data'];
  }
  return value;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}
