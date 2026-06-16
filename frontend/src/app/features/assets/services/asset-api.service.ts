import {
  HttpClient,
  HttpContext,
  HttpEvent,
  HttpEventType,
  HttpHeaders,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom, map, Observable, switchMap } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiEndpoints } from '@app/core/api/api-endpoints';
import {
  SKIP_APP_HEADERS,
  SKIP_AUTH,
  SKIP_ERROR_TOAST,
  SKIP_GLOBAL_LOADING,
  SKIP_REFRESH,
} from '@app/core/auth/auth-request-context';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
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
  readonly brandId: string | null;
  readonly productServiceId: string | null;
  readonly projectCampaignId: string | null;
  readonly storageFileId: string | null;
  readonly uploadedBy: string;
  readonly assetType: AssetType;
  readonly assetCategory: AssetCategory;
  readonly originalFileName: string;
  readonly storedFileName?: string | null;
  readonly fileType?: string | null;
  readonly mimeType?: string | null;
  readonly fileExtension?: string | null;
  readonly fileSize?: number | null;
  readonly storageProvider?: string | null;
  readonly storageBucket?: string | null;
  readonly storageKey?: string | null;
  readonly publicUrl?: string | null;
  readonly previewUrl?: string | null;
  readonly thumbnailUrl?: string | null;
  readonly publicPreviewUrl?: string | null;
  readonly signedPreviewUrl?: string | null;
  readonly downloadUrl?: string | null;
  readonly url?: string | null;
  readonly displayName: string;
  readonly description: string | null;
  readonly uploadSessionId: string | null;
  readonly previewStatus: PreviewStatus;
  readonly processingStatus: ProcessingStatus;
  readonly status: AssetStatus;
  readonly tags: readonly string[] | null;
  readonly metadata: Readonly<Record<string, unknown>> | null;
  readonly width?: number | null;
  readonly height?: number | null;
  readonly duration?: number | null;
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
  readonly url?: string | null;
  readonly previewUrl?: string | null;
  readonly downloadUrl?: string | null;
  readonly signedPreviewUrl?: string | null;
  readonly expiresAt?: string | null;
  readonly data?: AssetUrlResponseDto | null;
}

interface AssetUploadUrlResponseDto {
  readonly assetId: string;
  readonly uploadReferenceId: string | null;
  readonly uploadUrl: string;
  readonly method: string;
  readonly headers: Readonly<Record<string, string>>;
  readonly expiresAt: string;
  readonly maxFileSizeBytes: number;
}

interface CreateAssetUploadUrlRequestDto {
  readonly assetType: AssetType | null;
  readonly assetCategory: AssetCategory;
  readonly originalFileName: string;
  readonly contentType: string;
  readonly sizeBytes: number;
  readonly checksum: string | null;
  readonly displayName: string;
  readonly description: string | null;
  readonly tags: readonly string[];
  readonly metadata: Readonly<Record<string, unknown>>;
}

interface ConfirmAssetUploadRequestDto {
  readonly assetId: string;
  readonly uploadReferenceId: string | null;
  readonly checksum: string | null;
}

@Injectable({ providedIn: 'root' })
export class AssetApiService {
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
        ApiEndpoints.assets.projectList(workspaceId, projectId),
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

    const result = unwrapApiResponse(response);
    return {
      items: result.items.map(mapAsset),
      pagination: mapPagination(result),
    };
  }

  async getAsset(workspaceId: string, assetId: string, context?: HttpContext): Promise<Asset> {
    const response = await firstValueFrom(
      this.api.get<AssetViewDto>(ApiEndpoints.assets.detail(workspaceId, assetId), { context }),
    );

    return mapAsset(unwrapApiResponse(response));
  }

  async updateAsset(
    workspaceId: string,
    assetId: string,
    payload: UpdateAssetPayload,
  ): Promise<Asset> {
    const response = await firstValueFrom(
      this.api.put<AssetViewDto, UpdateAssetPayload>(
        ApiEndpoints.assets.detail(workspaceId, assetId),
        payload,
      ),
    );

    return mapAsset(unwrapApiResponse(response));
  }

  async deleteAsset(workspaceId: string, assetId: string): Promise<void> {
    await firstValueFrom(
      this.api.delete<void>(ApiEndpoints.assets.detail(workspaceId, assetId)),
    );
  }

  async getPreviewUrl(workspaceId: string, assetId: string, context?: HttpContext): Promise<AssetUrl> {
    const response = await firstValueFrom(
      this.api.get<AssetUrlResponseDto>(
        ApiEndpoints.assets.previewUrl(workspaceId, assetId),
        { context },
      ),
    );

    return normalizeAssetUrl(unwrapApiResponse(response), 'preview');
  }

  async getDownloadUrl(workspaceId: string, assetId: string): Promise<AssetUrl> {
    const response = await firstValueFrom(
      this.api.get<AssetUrlResponseDto>(
        ApiEndpoints.assets.downloadUrl(workspaceId, assetId),
      ),
    );
    return normalizeAssetUrl(unwrapApiResponse(response), 'download');
  }

  uploadProjectAsset(
    workspaceId: string,
    payload: UploadAssetPayload,
  ): Observable<AssetUploadEvent | null> {
    return this.api
      .post<AssetUploadUrlResponseDto, CreateAssetUploadUrlRequestDto>(
        ApiEndpoints.assets.projectUploadUrl(workspaceId, payload.projectId),
        {
          assetType: null,
          assetCategory: payload.assetCategory,
          originalFileName: payload.file.name,
          contentType: payload.file.type || 'application/octet-stream',
          sizeBytes: payload.file.size,
          checksum: null,
          displayName: payload.displayName.trim(),
          description: payload.description.trim() || null,
          tags: payload.tags,
          metadata: {
            fileSize: payload.file.size,
            mimeType: payload.file.type || 'application/octet-stream',
          },
        },
      )
      .pipe(
        switchMap(({ data }) => {
          const request = new HttpRequest(data.method || 'PUT', data.uploadUrl, payload.file, {
            reportProgress: true,
            headers: signedUploadHeaders(data.headers, payload.file),
            context: new HttpContext()
              .set(SKIP_AUTH, true)
              .set(SKIP_REFRESH, true)
              .set(SKIP_ERROR_TOAST, true)
              .set(SKIP_GLOBAL_LOADING, true)
              .set(SKIP_APP_HEADERS, true),
          });

          return this.http.request(request).pipe(
            switchMap((event) => {
              if (event.type === HttpEventType.UploadProgress) {
                return [mapUploadProgress(event)];
              }

              if (event instanceof HttpResponse) {
                return this.api
                  .post<AssetViewDto, ConfirmAssetUploadRequestDto>(
                    ApiEndpoints.assets.confirm(workspaceId),
                    {
                      assetId: data.assetId,
                      uploadReferenceId: data.uploadReferenceId,
                      checksum: null,
                    },
                  )
                  .pipe(map((response) => ({ kind: 'completed' as const, asset: mapAsset(response.data) })));
              }

              return [null];
            }),
          );
        }),
      );
  }
}

function mapUploadProgress(event: HttpEvent<unknown>): AssetUploadEvent {
  const total = event.type === HttpEventType.UploadProgress ? event.total ?? 0 : 0;
  const loaded = event.type === HttpEventType.UploadProgress ? event.loaded : 0;
  return {
    kind: 'progress',
    progress: total > 0 ? Math.round((loaded / total) * 100) : 0,
  };
}

function mapAsset(source: AssetViewDto): Asset {
  const metadata = source.metadata ?? {};
  const fileSize = source.fileSize ?? (typeof metadata['fileSize'] === 'number' ? metadata['fileSize'] : 0);
  const mimeType = source.mimeType ?? (typeof metadata['mimeType'] === 'string' ? metadata['mimeType'] : '');
  const provider = source.storageProvider ?? (typeof metadata['provider'] === 'string' ? metadata['provider'] : 'Private storage');
  const storageKey = source.storageKey ?? '';
  const fileExtension = source.fileExtension ?? extensionFromName(source.originalFileName);
  const width = source.width ?? (typeof metadata['width'] === 'number' ? metadata['width'] : null);
  const height = source.height ?? (typeof metadata['height'] === 'number' ? metadata['height'] : null);
  const duration = source.duration ?? (typeof metadata['duration'] === 'number' ? metadata['duration'] : null);

  return {
    id: source.id,
    workspaceId: source.workspaceId,
    brandId: source.brandId ?? '',
    productServiceId: source.productServiceId ?? '',
    projectCampaignId: source.projectCampaignId ?? '',
    storageFileId: source.storageFileId ?? '',
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
          id: source.storageFileId ?? source.id,
          workspaceId: source.workspaceId,
          provider,
          bucket: source.storageBucket ?? null,
          objectKey: storageKey,
          cdnUrl: null,
          mimeType,
          fileExtension,
          fileSize,
          hash: null,
          width,
          height,
          duration,
          storageClass: null,
          filePurpose: null,
          createdAt: source.createdAt,
          updatedAt: source.updatedAt,
        }
      : null,
    previewUrl: source.previewUrl ?? source.publicUrl ?? null,
    thumbnailUrl: source.thumbnailUrl ?? null,
    publicUrl: source.publicUrl ?? null,
    publicPreviewUrl: source.publicPreviewUrl ?? null,
    signedPreviewUrl: source.signedPreviewUrl ?? null,
    downloadUrl: source.downloadUrl ?? null,
    url: source.url ?? null,
  };
}

function extensionFromName(fileName: string): string {
  const parts = fileName.split('.');
  return parts.length > 1 ? parts[parts.length - 1]!.toLowerCase() : '';
}

function normalizeAssetUrl(
  source: AssetUrlResponseDto | null | undefined,
  preferred: 'preview' | 'download',
): AssetUrl {
  const nested = source?.data ?? null;
  const url =
    (preferred === 'download' ? source?.downloadUrl : source?.previewUrl) ||
    source?.url ||
    source?.signedPreviewUrl ||
    (preferred === 'download' ? nested?.downloadUrl : nested?.previewUrl) ||
    nested?.url ||
    nested?.signedPreviewUrl ||
    '';

  return {
    url,
    expiresAt: source?.expiresAt || nested?.expiresAt || '',
  };
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

function signedUploadHeaders(
  headers: Readonly<Record<string, string>> | null | undefined,
  file: File,
): HttpHeaders {
  let result = new HttpHeaders(headers ?? {});
  if (file.type && !result.has('Content-Type')) {
    result = result.set('Content-Type', file.type);
  }
  return result;
}

