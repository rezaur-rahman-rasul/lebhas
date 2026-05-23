import {
  HttpClient,
  HttpContext,
  HttpEvent,
  HttpEventType,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiResponse } from '@app/shared/models/api-response.model';
import { joinUrl } from '@app/shared/utils/join-url';
import { environment } from '@env/environment';
import {
  Asset,
  AssetActionResult,
  AssetCategory,
  AssetFileType,
  AssetFilter,
  AssetFolder,
  AssetPage,
  AssetPagination,
  AssetStatus,
  AssetUploadEvent,
  AssetUrl,
  CreateAssetFolderPayload,
  DEFAULT_ASSET_PAGINATION,
  UpdateAssetFolderPayload,
  UpdateAssetPayload,
  UploadAssetPayload,
} from '../models/asset.models';

type AssetApiStatus = 'PROCESSING' | 'ACTIVE' | 'FAILED' | 'DELETED';

interface AssetResponseDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly uploadedBy: string;
  readonly folderId: string | null;
  readonly originalFileName: string;
  readonly storedFileName: string | null;
  readonly fileType: AssetFileType;
  readonly mimeType: string;
  readonly fileExtension: string;
  readonly fileSize: number;
  readonly storageProvider: string;
  readonly storageBucket: string | null;
  readonly storageKey: string;
  readonly publicUrl: string | null;
  readonly previewUrl: string | null;
  readonly thumbnailUrl: string | null;
  readonly assetCategory: AssetCategory;
  readonly status: AssetApiStatus;
  readonly width: number | null;
  readonly height: number | null;
  readonly duration: number | null;
  readonly tags: readonly string[] | null;
  readonly metadata: Readonly<Record<string, unknown>> | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface AssetFolderResponseDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly name: string;
  readonly parentFolderId: string | null;
  readonly description: string | null;
  readonly createdBy: string | null;
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

  listAssets(
    workspaceId: string,
    filters: AssetFilter,
    page: number,
    size: number,
    context?: HttpContext,
  ) {
    return this.api
      .get<PagedResultDto<AssetResponseDto>>(`/api/v1/workspaces/${workspaceId}/assets`, {
        params: {
          assetCategory: filters.assetCategory,
          fileType: filters.fileType,
          folderId: filters.folderId,
          tag: filters.tag || null,
          uploadedBy: filters.uploadedBy,
          status: mapStatusToApi(filters.status),
          search: filters.search || null,
          createdFrom: filters.createdFrom,
          createdTo: filters.createdTo,
          page,
          size,
          sortBy: filters.sortBy,
          direction: filters.direction.toUpperCase(),
        },
        context,
      })
      .pipe(
        map(({ data }) => ({
          items: data.items.map(mapAsset),
          pagination: mapPagination(data),
        })),
      );
  }

  getAsset(workspaceId: string, assetId: string, context?: HttpContext) {
    return this.api
      .get<AssetResponseDto>(`/api/v1/workspaces/${workspaceId}/assets/${assetId}`, { context })
      .pipe(map(({ data }) => mapAsset(data)));
  }

  updateAsset(workspaceId: string, assetId: string, payload: UpdateAssetPayload) {
    return this.api
      .put<AssetResponseDto, UpdateAssetPayload>(
        `/api/v1/workspaces/${workspaceId}/assets/${assetId}`,
        payload,
      )
      .pipe(map(({ data }) => mapAsset(data)));
  }

  deleteAsset(workspaceId: string, assetId: string) {
    return this.api.delete<void>(`/api/v1/workspaces/${workspaceId}/assets/${assetId}`);
  }

  listFolders(workspaceId: string, context?: HttpContext) {
    return this.api
      .get<readonly AssetFolderResponseDto[]>(`/api/v1/workspaces/${workspaceId}/asset-folders`, {
        context,
      })
      .pipe(map(({ data }) => data.map(mapFolder)));
  }

  createFolder(workspaceId: string, payload: CreateAssetFolderPayload) {
    return this.api
      .post<AssetFolderResponseDto, CreateAssetFolderPayload>(
        `/api/v1/workspaces/${workspaceId}/asset-folders`,
        payload,
      )
      .pipe(map(({ data }) => mapFolder(data)));
  }

  updateFolder(workspaceId: string, folderId: string, payload: UpdateAssetFolderPayload) {
    return this.api
      .put<AssetFolderResponseDto, UpdateAssetFolderPayload>(
        `/api/v1/workspaces/${workspaceId}/asset-folders/${folderId}`,
        payload,
      )
      .pipe(map(({ data }) => mapFolder(data)));
  }

  deleteFolder(workspaceId: string, folderId: string) {
    return this.api.delete<void>(`/api/v1/workspaces/${workspaceId}/asset-folders/${folderId}`);
  }

  getPreviewUrl(workspaceId: string, assetId: string) {
    return this.api
      .get<AssetUrlResponseDto>(`/api/v1/workspaces/${workspaceId}/assets/${assetId}/preview-url`)
      .pipe(map(({ data }) => data));
  }

  getDownloadUrl(workspaceId: string, assetId: string) {
    return this.api
      .get<AssetUrlResponseDto>(
        `/api/v1/workspaces/${workspaceId}/assets/${assetId}/download-url`,
      )
      .pipe(map(({ data }) => data));
  }

  uploadAsset(workspaceId: string, payload: UploadAssetPayload) {
    const formData = new FormData();
    formData.append('file', payload.file);
    formData.append('assetCategory', payload.assetCategory);

    if (payload.folderId) {
      formData.append('folderId', payload.folderId);
    }

    if (payload.tags.length) {
      formData.append('tags', payload.tags.join(','));
    }

    if (Object.keys(payload.metadata).length) {
      formData.append('metadata', JSON.stringify(payload.metadata));
    }

    const request = new HttpRequest(
      'POST',
      joinUrl(environment.apiBaseUrl, `/api/v1/workspaces/${workspaceId}/assets/upload`),
      formData,
      { reportProgress: true },
    );

    return this.http.request<ApiResponse<AssetResponseDto>>(request).pipe(
      map((event) => mapUploadEvent(event)),
      map((event) => {
        if (!event) {
          return null;
        }

        return event;
      }),
      map((event) => event as AssetUploadEvent | null),
    );
  }
}

function mapAsset(source: AssetResponseDto): Asset {
  return {
    id: source.id,
    workspaceId: source.workspaceId,
    uploadedBy: source.uploadedBy,
    folderId: source.folderId,
    originalFileName: source.originalFileName,
    storedFileName: source.storedFileName,
    fileType: source.fileType,
    mimeType: source.mimeType,
    fileExtension: source.fileExtension,
    fileSize: source.fileSize,
    storageProvider: source.storageProvider,
    storageBucket: source.storageBucket,
    storageKey: source.storageKey,
    publicUrl: source.publicUrl,
    previewUrl: source.previewUrl,
    thumbnailUrl: source.thumbnailUrl,
    assetCategory: source.assetCategory,
    status: mapAssetStatus(source.status),
    width: source.width,
    height: source.height,
    duration: source.duration,
    tags: source.tags ?? [],
    metadata: source.metadata ?? {},
    createdAt: source.createdAt,
    updatedAt: source.updatedAt,
  };
}

function mapFolder(source: AssetFolderResponseDto): AssetFolder {
  return {
    id: source.id,
    workspaceId: source.workspaceId,
    name: source.name,
    parentFolderId: source.parentFolderId,
    description: source.description,
    createdBy: source.createdBy,
    createdAt: source.createdAt,
    updatedAt: source.updatedAt,
  };
}

function mapPagination(source: PagedResultDto<AssetResponseDto>): AssetPagination {
  return {
    page: source.page ?? DEFAULT_ASSET_PAGINATION.page,
    size: source.size ?? DEFAULT_ASSET_PAGINATION.size,
    totalItems: source.totalItems ?? DEFAULT_ASSET_PAGINATION.totalItems,
    totalPages: source.totalPages ?? DEFAULT_ASSET_PAGINATION.totalPages,
    first: source.first ?? DEFAULT_ASSET_PAGINATION.first,
    last: source.last ?? DEFAULT_ASSET_PAGINATION.last,
  };
}

function mapAssetStatus(status: AssetApiStatus): AssetStatus {
  switch (status) {
    case 'PROCESSING':
      return 'UPLOADING';
    case 'ACTIVE':
      return 'READY';
    case 'FAILED':
      return 'FAILED';
    case 'DELETED':
    default:
      return 'DELETED';
  }
}

function mapStatusToApi(status: AssetStatus | null): AssetApiStatus | null {
  switch (status) {
    case 'UPLOADING':
      return 'PROCESSING';
    case 'READY':
      return 'ACTIVE';
    case 'FAILED':
      return 'FAILED';
    case 'DELETED':
      return 'DELETED';
    default:
      return null;
  }
}

function mapUploadEvent(
  event: HttpEvent<ApiResponse<AssetResponseDto>>,
): AssetUploadEvent | null {
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
