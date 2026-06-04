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
import { catchError, filter, map, switchMap, throwError, timeout } from 'rxjs';

import { ApiEndpoints } from '@app/core/api/api-endpoints';
import { ApiService } from '@app/core/api/api.service';
import { environment } from '@env/environment';
import {
  SKIP_APP_HEADERS,
  SKIP_AUTH,
  SKIP_ERROR_TOAST,
  SKIP_GLOBAL_LOADING,
  SKIP_REFRESH,
} from '@app/core/auth/auth-request-context';
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

type AssetApiStatus =
  | 'UPLOAD_PENDING'
  | 'UPLOADING'
  | 'AVAILABLE'
  | 'READY'
  | 'FAILED'
  | 'DELETED'
  | 'GENERATED_METADATA_ONLY';

interface AssetResponseDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly brandId?: string | null;
  readonly productServiceId?: string | null;
  readonly projectCampaignId?: string | null;
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
  readonly deleted?: boolean | null;
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
  readonly method: string | null;
  readonly headers: Readonly<Record<string, string>> | null;
  readonly expiresAt: string;
  readonly maxFileSizeBytes: number;
}

interface CreateAssetUploadUrlRequestDto {
  readonly assetType: string | null;
  readonly assetCategory: AssetCategory;
  readonly folderId: string | null;
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
    const params = buildAssetListParams(filters, page, size);

    return this.api
      .get<PagedResultDto<AssetResponseDto>>(ApiEndpoints.assets.list(workspaceId), {
        params,
        context,
      })
      .pipe(
        map(({ data }) => {
          const items = normalizeAssetItems(extractAssetResponseItems(data));
          return {
            items,
            pagination: mapPagination(data, items.length),
          };
        }),
      );
  }

  getAsset(workspaceId: string, assetId: string, context?: HttpContext) {
    return this.api
      .get<AssetResponseDto>(ApiEndpoints.assets.detail(workspaceId, assetId), { context })
      .pipe(map(({ data }) => mapAsset(data)));
  }

  updateAsset(workspaceId: string, assetId: string, payload: UpdateAssetPayload) {
    return this.api
      .put<AssetResponseDto, UpdateAssetPayload>(
        ApiEndpoints.assets.detail(workspaceId, assetId),
        payload,
      )
      .pipe(map(({ data }) => mapAsset(data)));
  }

  deleteAsset(workspaceId: string, assetId: string) {
    return this.api
      .delete<void>(ApiEndpoints.assets.detail(workspaceId, assetId))
      .pipe(timeout({ first: 8000 }));
  }

  listFolders(workspaceId: string, context?: HttpContext) {
    return this.api
      .get<readonly AssetFolderResponseDto[]>(ApiEndpoints.assets.folders(workspaceId), {
        context,
      })
      .pipe(map(({ data }) => data.map(mapFolder)));
  }

  createFolder(workspaceId: string, payload: CreateAssetFolderPayload) {
    return this.api
      .post<AssetFolderResponseDto, CreateAssetFolderPayload>(
        ApiEndpoints.assets.folders(workspaceId),
        payload,
      )
      .pipe(map(({ data }) => mapFolder(data)));
  }

  updateFolder(workspaceId: string, folderId: string, payload: UpdateAssetFolderPayload) {
    return this.api
      .put<AssetFolderResponseDto, UpdateAssetFolderPayload>(
        ApiEndpoints.assets.folder(workspaceId, folderId),
        payload,
      )
      .pipe(map(({ data }) => mapFolder(data)));
  }

  deleteFolder(workspaceId: string, folderId: string) {
    return this.api.delete<void>(ApiEndpoints.assets.folder(workspaceId, folderId));
  }

  getPreviewUrl(workspaceId: string, assetId: string, context?: HttpContext) {
    return this.api
      .get<AssetUrlResponseDto>(ApiEndpoints.assets.previewUrl(workspaceId, assetId), { context })
      .pipe(
        timeout({ first: 15000 }),
        map(({ data }) => normalizeAssetUrl(data, 'preview')),
      );
  }

  getDownloadUrl(workspaceId: string, assetId: string, context?: HttpContext) {
    return this.api
      .get<AssetUrlResponseDto>(
        ApiEndpoints.assets.downloadUrl(workspaceId, assetId),
        { context },
      )
      .pipe(
        timeout({ first: 15000 }),
        map(({ data }) => normalizeAssetUrl(data, 'download')),
      );
  }

  uploadAsset(workspaceId: string, payload: UploadAssetPayload) {
    const formData = buildUploadFormData(payload);
    logUploadDiagnostics(ApiEndpoints.assets.upload(workspaceId), payload.file);

    return this.api
      .post<AssetResponseDto, FormData>(
        ApiEndpoints.assets.upload(workspaceId),
        formData,
        {
          context: new HttpContext().set(SKIP_ERROR_TOAST, true),
        },
      )
      .pipe(map(({ data }) => ({ kind: 'completed', asset: mapAsset(data) }) as AssetUploadEvent));
  }

  listProjectAssets(
    workspaceId: string,
    projectId: string,
    filters: AssetFilter,
    page: number,
    size: number,
    context?: HttpContext,
  ) {
    const params = buildAssetListParams(filters, page, size);

    return this.api
      .get<PagedResultDto<AssetResponseDto>>(ApiEndpoints.assets.projectList(workspaceId, projectId), {
        params,
        context,
      })
      .pipe(
        map(({ data }) => {
          const items = normalizeAssetItems(extractAssetResponseItems(data));
          return {
            items,
            pagination: mapPagination(data, items.length),
          };
        }),
      );
  }

  uploadProjectAsset(workspaceId: string, projectId: string, payload: UploadAssetPayload) {
    const formData = buildUploadFormData(payload);
    formData.append('projectId', projectId);

    logUploadDiagnostics(ApiEndpoints.assets.projectUpload(workspaceId, projectId), payload.file);

    return this.api
      .post<AssetResponseDto, FormData>(
        ApiEndpoints.assets.projectUpload(workspaceId, projectId),
        formData,
        {
          context: new HttpContext().set(SKIP_ERROR_TOAST, true),
        },
      )
      .pipe(map(({ data }) => ({ kind: 'completed', asset: mapAsset(data) }) as AssetUploadEvent));
  }

  uploadProjectAssetSigned(workspaceId: string, projectId: string, payload: UploadAssetPayload) {
    return this.api
      .post<AssetUploadUrlResponseDto, CreateAssetUploadUrlRequestDto>(
        ApiEndpoints.assets.projectUploadUrl(workspaceId, projectId),
        {
          assetType: payload.assetCategory === 'PRODUCT_IMAGE' || payload.assetCategory === 'REFERENCE_IMAGE'
            ? 'RAW_IMAGE'
            : null,
          assetCategory: payload.assetCategory,
          folderId: payload.folderId,
          originalFileName: payload.file.name,
          contentType: payload.file.type || 'application/octet-stream',
          sizeBytes: payload.file.size,
          checksum: null,
          displayName: payload.file.name,
          description: null,
          tags: payload.tags,
          metadata: payload.metadata,
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
            catchError((error) => {
              if (isLikelyBrowserNetworkBlock(error)) {
                return throwError(
                  () =>
                    new Error(
                      'Cloudflare R2 blocked the browser upload. Configure R2 bucket CORS to allow PUT from http://localhost:4200, then retry.',
                    ),
                );
              }

              return throwError(() => error);
            }),
            map((event) => mapSignedUploadProgress(event)),
            filter((event): event is AssetUploadEvent => event !== null),
            switchMap((event) => {
              if (event.kind === 'progress') {
                return [event];
              }

              return this.api
                .post<AssetResponseDto, ConfirmAssetUploadRequestDto>(
                  ApiEndpoints.assets.confirm(workspaceId),
                  {
                    assetId: data.assetId,
                    uploadReferenceId: data.uploadReferenceId,
                    checksum: null,
                  },
                )
                .pipe(map((response) => ({ kind: 'completed', asset: mapAsset(response.data) }) as const));
            }),
          );
        }),
      );
  }
}

function normalizeAssetItems(items: readonly AssetResponseDto[] | null | undefined): readonly Asset[] {
  return (items ?? [])
    .filter(isValidAssetResponse)
    .map(mapAsset)
    .filter(isRealAsset);
}

function extractAssetResponseItems(source: unknown): readonly AssetResponseDto[] {
  if (Array.isArray(source)) {
    return source as readonly AssetResponseDto[];
  }
  if (!source || typeof source !== 'object') {
    return [];
  }

  const record = source as Record<string, unknown>;
  if (Array.isArray(record['items'])) {
    return record['items'] as readonly AssetResponseDto[];
  }
  if (Array.isArray(record['content'])) {
    return record['content'] as readonly AssetResponseDto[];
  }
  const nestedData = record['data'];
  if (Array.isArray(nestedData)) {
    return nestedData as readonly AssetResponseDto[];
  }
  if (nestedData && typeof nestedData === 'object') {
    return extractAssetResponseItems(nestedData);
  }

  return [];
}

function isValidAssetResponse(source: AssetResponseDto | null | undefined): source is AssetResponseDto {
  return Boolean(
    source &&
      typeof source.id === 'string' &&
      source.id.trim() &&
      source.deleted !== true &&
      source.status !== 'DELETED',
  );
}

function mapAsset(source: AssetResponseDto): Asset {
  return {
    id: source.id,
    workspaceId: source.workspaceId,
    uploadedBy: source.uploadedBy,
    projectId: source.projectCampaignId,
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

function mapPagination(source: PagedResultDto<AssetResponseDto>, itemCount?: number): AssetPagination {
  const record = source as unknown as Record<string, unknown>;
  const totalElements = typeof record['totalElements'] === 'number' ? record['totalElements'] : null;
  const totalItems = source.totalItems ?? totalElements ?? itemCount ?? DEFAULT_ASSET_PAGINATION.totalItems;
  return {
    page: source.page ?? DEFAULT_ASSET_PAGINATION.page,
    size: source.size ?? DEFAULT_ASSET_PAGINATION.size,
    totalItems,
    totalPages: source.totalPages ?? DEFAULT_ASSET_PAGINATION.totalPages,
    first: source.first ?? DEFAULT_ASSET_PAGINATION.first,
    last: source.last ?? DEFAULT_ASSET_PAGINATION.last,
  };
}

function mapAssetStatus(status: AssetApiStatus): AssetStatus {
  switch (status) {
    case 'UPLOAD_PENDING':
    case 'UPLOADING':
      return 'UPLOADING';
    case 'AVAILABLE':
      return 'AVAILABLE';
    case 'READY':
      return 'READY';
    case 'GENERATED_METADATA_ONLY':
      return 'GENERATED_METADATA_ONLY';
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
      return 'UPLOADING';
    case 'AVAILABLE':
      return 'AVAILABLE';
    case 'READY':
      return 'READY';
    case 'GENERATED_METADATA_ONLY':
      return 'GENERATED_METADATA_ONLY';
    case 'FAILED':
      return 'FAILED';
    case 'DELETED':
      return 'DELETED';
    default:
      return null;
  }
}

function buildAssetListParams(filters: AssetFilter, page: number, size: number) {
  const search = filters.search.trim();

  return {
    assetCategory: filters.assetCategory,
    uploadedBy: filters.uploadedBy,
    status: mapStatusToApi(filters.status),
    search: search || null,
    createdFrom: filters.createdFrom,
    createdTo: filters.createdTo,
    page: Number.isFinite(page) && page >= 0 ? page : DEFAULT_ASSET_PAGINATION.page,
    size: Number.isFinite(size) && size > 0 ? size : DEFAULT_ASSET_PAGINATION.size,
    sortBy: mapSortField(filters.sortBy),
    direction: filters.direction.toUpperCase(),
  };
}

function isRealAsset(asset: Asset): boolean {
  return Boolean(asset.id?.trim()) && asset.status !== 'DELETED';
}

function mapSortField(sortBy: AssetFilter['sortBy']): string {
  switch (sortBy) {
    case 'updatedAt':
      return 'updatedAt';
    case 'originalFileName':
      return 'displayName';
    case 'createdAt':
    case 'fileSize':
    default:
      return 'createdAt';
  }
}

function mapSignedUploadProgress(event: HttpEvent<unknown>): AssetUploadEvent | null {
  if (event.type === HttpEventType.UploadProgress) {
    const total = event.total ?? 0;
    return {
      kind: 'progress',
      progress: total > 0 ? Math.round((event.loaded / total) * 100) : 0,
    };
  }

  if (event instanceof HttpResponse) {
    return {
      kind: 'completed',
      asset: null as unknown as Asset,
    };
  }

  return null;
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

function isLikelyBrowserNetworkBlock(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'status' in error && error.status === 0;
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

function stringMetadata(
  metadata: Readonly<Record<string, unknown>>,
  key: string,
): string | null {
  const value = metadata[key];
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}

function buildUploadFormData(payload: UploadAssetPayload): FormData {
  const formData = new FormData();
  formData.append('file', payload.file);
  formData.append('assetCategory', payload.assetCategory);
  formData.append('assetType', uploadAssetType(payload.assetCategory));
  formData.append('displayName', payload.file.name);

  const brandId = stringMetadata(payload.metadata, 'brandId');
  const productId = stringMetadata(payload.metadata, 'productId');
  if (brandId) {
    formData.append('brandId', brandId);
  }
  if (productId) {
    formData.append('productId', productId);
  }

  if (payload.tags.length > 0) {
    formData.append('tags', payload.tags.join(','));
  }

  formData.append(
    'metadata',
    JSON.stringify(compactMetadata({
      ...payload.metadata,
      folderId: payload.folderId,
    })),
  );

  return formData;
}

function compactMetadata(metadata: Readonly<Record<string, unknown>>): Readonly<Record<string, unknown>> {
  return Object.fromEntries(
    Object.entries(metadata).filter(([, value]) => value !== null && value !== undefined && value !== ''),
  );
}

function uploadAssetType(category: AssetCategory): string {
  switch (category) {
    case 'PRODUCT_IMAGE':
      return 'PRODUCT_IMAGE';
    case 'BRAND_LOGO':
      return 'BRAND_LOGO';
    case 'PACKAGING_IMAGE':
      return 'PACKAGING_IMAGE';
    case 'EXPORT_IMAGE':
      return 'EXPORT_IMAGE';
    case 'EXPORT_VIDEO':
    case 'PRODUCT_VIDEO':
      return 'EXPORT_VIDEO';
    case 'REFERENCE_IMAGE':
    case 'REFERENCE_VIDEO':
    case 'REFERENCE_ASSET':
      return 'REFERENCE_ASSET';
    default:
      return 'RAW_IMAGE';
  }
}

function logUploadDiagnostics(endpoint: string, file: File): void {
  if (environment.production) {
    return;
  }

  console.info('[asset-upload]', {
    endpoint,
    fileName: file.name,
    fileSize: file.size,
    contentType: file.type || 'application/octet-stream',
  });
}
