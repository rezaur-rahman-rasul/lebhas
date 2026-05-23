import {
  HttpClient,
  HttpContext,
  HttpEvent,
  HttpEventType,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom, map, Observable } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiResponse } from '@app/shared/models/api-response.model';
import { joinUrl } from '@app/shared/utils/join-url';
import { environment } from '@env/environment';
import {
  Asset,
  AssetCategory,
  AssetFilter,
  AssetPage,
  AssetPagination,
  PreviewStatus,
  ProcessingStatus,
  AssetStatus,
  AssetType,
  AssetUploadEvent,
  AssetUrl,
  DEFAULT_ASSET_PAGINATION,
  UpdateAssetPayload,
  UploadAssetPayload,
} from '../models/asset.models';

interface AssetViewDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly brandId: string;
  readonly productServiceId: string;
  readonly projectCampaignId: string;
  readonly storageFileId: string;
  readonly uploadedBy: string;
  readonly assetType: AssetType;
  readonly assetCategory: AssetCategory;
  readonly originalFileName: string;
  readonly displayName: string;
  readonly description: string | null;
  readonly uploadSessionId: string | null;
  readonly previewStatus: PreviewStatus;
  readonly processingStatus: ProcessingStatus;
  readonly status: AssetStatus;
  readonly tags: readonly string[] | null;
  readonly metadata: Readonly<Record<string, unknown>> | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface PagedResultDto<T> {
  readonly items: readonly T[];
  readonly totalItems: number;
  readonly totalPages: number;
  readonly page: number;
  readonly size: number;
  readonly first: boolean;
  readonly last: boolean;
}

interface AssetUrlResponseDto {
  readonly url: string;
  readonly expiresAt: string;
}

@Injectable({ providedIn: 'root' })
export class AssetService {
  private readonly api = inject(ApiService);
  private readonly http = inject(HttpClient);

  async listProjectAssets(
    workspaceId: string,
    projectId: string,
    filters: AssetFilter,
    page: number,
    size: number,
    context?: HttpContext,
  ): Promise<AssetPage> {
    const response = await firstValueFrom(
      this.api.get<PagedResultDto<AssetViewDto>>(
        `/api/v1/workspaces/${workspaceId}/projects/${projectId}/assets`,
        {
          params: {
            assetType: filters.assetType,
            assetCategory: filters.assetCategory,
            previewStatus: filters.previewStatus,
            processingStatus: filters.processingStatus,
            status: filters.status,
            search: filters.search || null,
            createdFrom: filters.createdFrom,
            createdTo: filters.createdTo,
            page,
            size,
            sortBy: filters.sortBy,
            direction: filters.direction.toUpperCase(),
          },
          context,
        },
      ),
    );

    return {
      items: response.data.items.map(mapAsset),
      pagination: mapPagination(response.data),
    };
  }

  async getAsset(workspaceId: string, assetId: string, context?: HttpContext): Promise<Asset> {
    const response = await firstValueFrom(
      this.api.get<AssetViewDto>(`/api/v1/workspaces/${workspaceId}/assets/${assetId}`, { context }),
    );

    return mapAsset(response.data);
  }

  async updateAsset(
    workspaceId: string,
    assetId: string,
    payload: UpdateAssetPayload,
  ): Promise<Asset> {
    const response = await firstValueFrom(
      this.api.put<AssetViewDto, UpdateAssetPayload>(
        `/api/v1/workspaces/${workspaceId}/assets/${assetId}`,
        payload,
      ),
    );

    return mapAsset(response.data);
  }

  async deleteAsset(workspaceId: string, assetId: string): Promise<void> {
    await firstValueFrom(
      this.api.delete<void>(`/api/v1/workspaces/${workspaceId}/assets/${assetId}`),
    );
  }

  async getPreviewUrl(workspaceId: string, assetId: string): Promise<AssetUrl> {
    const response = await firstValueFrom(
      this.api.get<AssetUrlResponseDto>(
        `/api/v1/workspaces/${workspaceId}/assets/${assetId}/preview-url`,
      ),
    );

    return response.data;
  }

  async getDownloadUrl(workspaceId: string, assetId: string): Promise<AssetUrl> {
    const response = await firstValueFrom(
      this.api.get<AssetUrlResponseDto>(
        `/api/v1/workspaces/${workspaceId}/assets/${assetId}/download-url`,
      ),
    );
    return response.data;
  }

  uploadProjectAsset(
    workspaceId: string,
    payload: UploadAssetPayload,
  ): Observable<AssetUploadEvent | null> {
    const formData = new FormData();
    formData.append('file', payload.file);
    formData.append('assetCategory', payload.assetCategory);
    formData.append('displayName', payload.displayName.trim());

    if (payload.description.trim()) {
      formData.append('description', payload.description.trim());
    }

    if (payload.tags.length) {
      formData.append('tags', payload.tags.join(','));
    }

    const request = new HttpRequest(
      'POST',
      joinUrl(
        environment.apiBaseUrl,
        `/api/v1/workspaces/${workspaceId}/projects/${payload.projectId}/assets/upload`,
      ),
      formData,
      { reportProgress: true },
    );

    return this.http.request<ApiResponse<AssetViewDto>>(request).pipe(
      map((event) => mapUploadEvent(event)),
    );
  }
}

function mapAsset(source: AssetViewDto): Asset {
  const metadata = source.metadata ?? {};
  const fileSize = typeof metadata['fileSize'] === 'number' ? metadata['fileSize'] : 0;
  const mimeType = typeof metadata['mimeType'] === 'string' ? metadata['mimeType'] : '';

  return {
    id: source.id,
    workspaceId: source.workspaceId,
    brandId: source.brandId,
    productServiceId: source.productServiceId,
    projectCampaignId: source.projectCampaignId,
    storageFileId: source.storageFileId,
    uploadedBy: source.uploadedBy,
    assetType: source.assetType,
    assetCategory: source.assetCategory,
    originalFileName: source.originalFileName,
    displayName: source.displayName,
    description: source.description,
    tags: source.tags ?? [],
    uploadSessionId: source.uploadSessionId,
    previewStatus: source.previewStatus,
    processingStatus: source.processingStatus,
    status: source.status,
    createdAt: source.createdAt,
    updatedAt: source.updatedAt,
    storageFile: fileSize
      ? {
          id: source.storageFileId,
          workspaceId: source.workspaceId,
          provider: 'unknown',
          bucket: null,
          objectKey: '',
          cdnUrl: null,
          mimeType,
          fileExtension: extensionFromName(source.originalFileName),
          fileSize,
          hash: null,
          width: null,
          height: null,
          duration: null,
          storageClass: null,
          filePurpose: null,
          createdAt: source.createdAt,
          updatedAt: source.updatedAt,
        }
      : null,
  };
}

function extensionFromName(fileName: string): string {
  const parts = fileName.split('.');
  return parts.length > 1 ? parts[parts.length - 1]!.toLowerCase() : '';
}

function mapPagination(source: PagedResultDto<AssetViewDto>): AssetPagination {
  return {
    page: source.page ?? DEFAULT_ASSET_PAGINATION.page,
    size: source.size ?? DEFAULT_ASSET_PAGINATION.size,
    totalItems: source.totalItems ?? DEFAULT_ASSET_PAGINATION.totalItems,
    totalPages: source.totalPages ?? DEFAULT_ASSET_PAGINATION.totalPages,
    first: source.first ?? DEFAULT_ASSET_PAGINATION.first,
    last: source.last ?? DEFAULT_ASSET_PAGINATION.last,
  };
}

function mapUploadEvent(event: HttpEvent<ApiResponse<AssetViewDto>>): AssetUploadEvent | null {
  if (event.type === HttpEventType.UploadProgress) {
    const total = event.total ?? 0;
    return {
      kind: 'progress',
      progress: total > 0 ? Math.round((event.loaded / total) * 100) : 0,
    };
  }

  if (event instanceof HttpResponse && event.body?.data) {
    return {
      kind: 'completed',
      asset: mapAsset(event.body.data),
    };
  }

  return null;
}
