export type AssetType = 'IMAGE' | 'VIDEO' | 'DOCUMENT' | 'OTHER';
export type AssetCategory =
  | 'PRODUCT_IMAGE'
  | 'PRODUCT_VIDEO'
  | 'BRAND_LOGO'
  | 'RAW_IMAGE'
  | 'RAW_VIDEO'
  | 'GENERATED_IMAGE'
  | 'GENERATED_VIDEO'
  | 'THUMBNAIL'
  | 'OTHER';
export type AssetStatus = 'UPLOADING' | 'READY' | 'FAILED' | 'DELETED';
export type PreviewStatus = 'PENDING' | 'READY' | 'FAILED';
export type ProcessingStatus = 'PENDING' | 'PROCESSING' | 'READY' | 'FAILED';
export type AssetViewMode = 'grid' | 'list';
export type AssetSortField = 'createdAt' | 'updatedAt' | 'originalFileName' | 'displayName';
export type AssetSortDirection = 'asc' | 'desc';
export type UploadState = 'idle' | 'uploading' | 'processing' | 'completed' | 'failed';

export interface StorageFile {
  readonly id: string;
  readonly workspaceId: string;
  readonly provider: string;
  readonly bucket: string | null;
  readonly objectKey: string;
  readonly cdnUrl: string | null;
  readonly mimeType: string;
  readonly fileExtension: string;
  readonly fileSize: number;
  readonly hash: string | null;
  readonly width: number | null;
  readonly height: number | null;
  readonly duration: number | null;
  readonly storageClass: string | null;
  readonly filePurpose: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface Asset {
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
  readonly tags: readonly string[];
  readonly uploadSessionId: string | null;
  readonly previewStatus: PreviewStatus;
  readonly processingStatus: ProcessingStatus;
  readonly status: AssetStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly storageFile?: StorageFile | null;
  readonly previewUrl?: string | null;
  readonly thumbnailUrl?: string | null;
}

export interface AssetFilter {
  readonly assetType: AssetType | null;
  readonly assetCategory: AssetCategory | null;
  readonly previewStatus: PreviewStatus | null;
  readonly processingStatus: ProcessingStatus | null;
  readonly status: AssetStatus | null;
  readonly search: string;
  readonly createdFrom: string | null;
  readonly createdTo: string | null;
  readonly sortBy: AssetSortField;
  readonly direction: AssetSortDirection;
}

export interface AssetPagination {
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
  readonly first: boolean;
  readonly last: boolean;
}

export interface AssetPage {
  readonly items: readonly Asset[];
  readonly pagination: AssetPagination;
}

export interface UploadAssetPayload {
  readonly projectId: string;
  readonly file: File;
  readonly assetCategory: AssetCategory;
  readonly displayName: string;
  readonly description: string;
  readonly tags: readonly string[];
}

export interface UpdateAssetPayload {
  readonly displayName?: string;
  readonly description?: string | null;
  readonly assetCategory?: AssetCategory;
  readonly tags?: readonly string[];
}

export interface AssetUrl {
  readonly url: string;
  readonly expiresAt: string;
}

export interface AssetActionResult {
  readonly ok: boolean;
  readonly message?: string;
  readonly fieldErrors: Readonly<Record<string, string>>;
}

export interface AssetUploadProgress {
  readonly kind: 'progress';
  readonly progress: number;
}

export interface AssetUploadCompleted {
  readonly kind: 'completed';
  readonly asset: Asset;
}

export type AssetUploadEvent = AssetUploadProgress | AssetUploadCompleted;

export const MAX_DISPLAY_NAME_LENGTH = 120;
export const MAX_TAG_LENGTH = 80;
export const MAX_DESCRIPTION_LENGTH = 500;
export const ASSET_LIST_PAGE_SIZE = 24;
export const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
export const MAX_VIDEO_BYTES = 200 * 1024 * 1024;
export const MAX_LOGO_BYTES = 5 * 1024 * 1024;

export const DEFAULT_ASSET_FILTERS: AssetFilter = {
  assetType: null,
  assetCategory: null,
  previewStatus: null,
  processingStatus: null,
  status: null,
  search: '',
  createdFrom: null,
  createdTo: null,
  sortBy: 'createdAt',
  direction: 'desc',
};

export const DEFAULT_ASSET_PAGINATION: AssetPagination = {
  page: 0,
  size: ASSET_LIST_PAGE_SIZE,
  totalItems: 0,
  totalPages: 0,
  first: true,
  last: true,
};

export const ASSET_CATEGORY_OPTIONS = [
  { value: 'PRODUCT_IMAGE' as const, label: 'Product image' },
  { value: 'PRODUCT_VIDEO' as const, label: 'Product video' },
  { value: 'BRAND_LOGO' as const, label: 'Brand logo' },
  { value: 'RAW_IMAGE' as const, label: 'Raw image' },
  { value: 'RAW_VIDEO' as const, label: 'Raw video' },
  { value: 'OTHER' as const, label: 'Other' },
] as const;

export const ASSET_TYPE_OPTIONS = [
  { value: 'IMAGE' as const, label: 'Image' },
  { value: 'VIDEO' as const, label: 'Video' },
  { value: 'DOCUMENT' as const, label: 'Document' },
  { value: 'OTHER' as const, label: 'Other' },
] as const;

export const ASSET_STATUS_OPTIONS = [
  { value: 'READY' as const, label: 'Ready' },
  { value: 'UPLOADING' as const, label: 'Uploading' },
  { value: 'FAILED' as const, label: 'Failed' },
] as const;

export function assetCategoryLabel(category: AssetCategory): string {
  return ASSET_CATEGORY_OPTIONS.find((option) => option.value === category)?.label ?? category;
}

export function assetTypeLabel(type: AssetType): string {
  return ASSET_TYPE_OPTIONS.find((option) => option.value === type)?.label ?? type;
}

export function assetStatusLabel(status: AssetStatus): string {
  return ASSET_STATUS_OPTIONS.find((option) => option.value === status)?.label ?? status;
}

export function assetStatusTone(status: AssetStatus): 'brand' | 'blue' | 'red' | 'neutral' {
  switch (status) {
    case 'READY':
      return 'brand';
    case 'UPLOADING':
      return 'blue';
    case 'FAILED':
      return 'red';
    default:
      return 'neutral';
  }
}

export function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) {
    return '0 B';
  }

  const units = ['B', 'KB', 'MB', 'GB'];
  let value = bytes;
  let unitIndex = 0;

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }

  return `${value >= 10 || unitIndex === 0 ? value.toFixed(0) : value.toFixed(1)} ${units[unitIndex]}`;
}

export function resolveFileSize(asset: Asset): number {
  return asset.storageFile?.fileSize ?? 0;
}

export function isImageAsset(asset: Pick<Asset, 'assetType'>): boolean {
  return asset.assetType === 'IMAGE';
}

export function isVideoAsset(asset: Pick<Asset, 'assetType'>): boolean {
  return asset.assetType === 'VIDEO';
}

export function isPreviewableAsset(asset: Pick<Asset, 'assetType'>): boolean {
  return isImageAsset(asset) || isVideoAsset(asset);
}

export const ASSET_UPLOAD_CATEGORY_OPTIONS: readonly {
  readonly value: AssetCategory;
  readonly label: string;
  readonly description: string;
}[] = [
  {
    value: 'PRODUCT_IMAGE',
    label: 'Product image',
    description: 'Product photography and catalog visuals for campaigns.',
  },
  {
    value: 'PRODUCT_VIDEO',
    label: 'Product video',
    description: 'Product motion clips for ad-ready video workflows.',
  },
  {
    value: 'BRAND_LOGO',
    label: 'Brand logo',
    description: 'Approved brand marks for template and creative placement.',
  },
  {
    value: 'RAW_IMAGE',
    label: 'Raw image',
    description: 'Unprocessed images from shoots, phones, or source collections.',
  },
  {
    value: 'RAW_VIDEO',
    label: 'Raw video',
    description: 'Source footage before editing or future creative processing.',
  },
  {
    value: 'OTHER',
    label: 'Other asset',
    description: 'Supporting files that do not fit a primary media category.',
  },
];

export const MAX_ASSET_TAG_LENGTH = MAX_TAG_LENGTH;

export function normalizeAssetTag(value: string): string {
  return value.trim().toLowerCase();
}

export function assetFileExtension(asset: Asset): string {
  return asset.storageFile?.fileExtension ?? extensionFromName(asset.originalFileName);
}

function extensionFromName(fileName: string): string {
  const parts = fileName.split('.');
  return parts.length > 1 ? parts[parts.length - 1]!.toLowerCase() : '';
}
