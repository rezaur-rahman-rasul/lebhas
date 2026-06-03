export type AssetCategory =
  | 'PRODUCT_IMAGE'
  | 'PRODUCT_VIDEO'
  | 'PACKAGING_IMAGE'
  | 'BRAND_LOGO'
  | 'REFERENCE_IMAGE'
  | 'REFERENCE_VIDEO'
  | 'REFERENCE_ASSET'
  | 'EXPORT_IMAGE'
  | 'EXPORT_VIDEO'
  | 'OTHER';

export type AssetFileType = 'IMAGE' | 'VIDEO' | 'VECTOR_IMAGE';
export type AssetStatus = 'UPLOADING' | 'AVAILABLE' | 'READY' | 'FAILED' | 'DELETED' | 'GENERATED_METADATA_ONLY';
export type AssetViewMode = 'grid' | 'list';
export type AssetSortField = 'createdAt' | 'updatedAt' | 'originalFileName' | 'fileSize';
export type AssetSortDirection = 'asc' | 'desc';
export type AssetFolderSelection = 'all' | 'uncategorized' | string;

export interface Asset {
  readonly id: string;
  readonly workspaceId: string;
  readonly uploadedBy: string;
  readonly projectId?: string | null;
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
  readonly status: AssetStatus;
  readonly width: number | null;
  readonly height: number | null;
  readonly duration: number | null;
  readonly tags: readonly string[];
  readonly metadata: Readonly<Record<string, unknown>>;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface AssetFolder {
  readonly id: string;
  readonly workspaceId: string;
  readonly name: string;
  readonly parentFolderId: string | null;
  readonly description: string | null;
  readonly createdBy: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface AssetFilter {
  readonly assetCategory: AssetCategory | null;
  readonly fileType: AssetFileType | null;
  readonly folderId: string | null;
  readonly tag: string;
  readonly uploadedBy: string | null;
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
  readonly file: File;
  readonly assetCategory: AssetCategory;
  readonly folderId: string | null;
  readonly tags: readonly string[];
  readonly metadata: Readonly<Record<string, unknown>>;
}

export interface UpdateAssetPayload {
  readonly folderId?: string | null;
  readonly assetCategory?: AssetCategory | null;
  readonly tags?: readonly string[];
  readonly metadata?: Readonly<Record<string, unknown>>;
}

export interface CreateAssetFolderPayload {
  readonly name: string;
  readonly parentFolderId: string | null;
  readonly description: string;
}

export interface UpdateAssetFolderPayload extends CreateAssetFolderPayload {}

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

export const MAX_ASSET_TAG_LENGTH = 80;
export const ASSET_LIST_PAGE_SIZE = 20;

export const DEFAULT_ASSET_FILTERS: AssetFilter = {
  assetCategory: null,
  fileType: null,
  folderId: null,
  tag: '',
  uploadedBy: null,
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

export const ASSET_CATEGORY_OPTIONS: readonly {
  readonly value: AssetCategory;
  readonly label: string;
}[] = [
  { value: 'PRODUCT_IMAGE', label: 'Product image' },
  { value: 'PRODUCT_VIDEO', label: 'Product video' },
  { value: 'PACKAGING_IMAGE', label: 'Packaging image' },
  { value: 'BRAND_LOGO', label: 'Brand logo' },
  { value: 'REFERENCE_IMAGE', label: 'Reference image' },
  { value: 'REFERENCE_VIDEO', label: 'Reference video' },
  { value: 'REFERENCE_ASSET', label: 'Reference asset' },
  { value: 'EXPORT_IMAGE', label: 'Export image' },
  { value: 'EXPORT_VIDEO', label: 'Export video' },
  { value: 'OTHER', label: 'Other' },
];

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
    value: 'REFERENCE_IMAGE',
    label: 'Reference image',
    description: 'Source images from shoots, phones, or campaign references.',
  },
  {
    value: 'REFERENCE_VIDEO',
    label: 'Reference video',
    description: 'Source motion clips for future creative processing.',
  },
  {
    value: 'OTHER',
    label: 'Other asset',
    description: 'Supporting files that do not fit a primary media category.',
  },
];

export const ASSET_FILE_TYPE_OPTIONS: readonly {
  readonly value: AssetFileType;
  readonly label: string;
}[] = [
  { value: 'IMAGE', label: 'Image' },
  { value: 'VECTOR_IMAGE', label: 'Vector image' },
  { value: 'VIDEO', label: 'Video' },
];

export const ASSET_STATUS_OPTIONS: readonly {
  readonly value: AssetStatus;
  readonly label: string;
}[] = [
  { value: 'READY', label: 'Ready' },
  { value: 'AVAILABLE', label: 'Available' },
  { value: 'UPLOADING', label: 'Uploading' },
  { value: 'GENERATED_METADATA_ONLY', label: 'Metadata only' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'DELETED', label: 'Deleted' },
];

export const ASSET_SORT_OPTIONS: readonly {
  readonly value: AssetSortField;
  readonly label: string;
}[] = [
  { value: 'createdAt', label: 'Newest first' },
  { value: 'updatedAt', label: 'Recently updated' },
  { value: 'originalFileName', label: 'File name' },
  { value: 'fileSize', label: 'File size' },
];

export const ASSET_CATEGORY_LABELS: Record<AssetCategory, string> = {
  PRODUCT_IMAGE: 'Product image',
  PRODUCT_VIDEO: 'Product video',
  PACKAGING_IMAGE: 'Packaging image',
  BRAND_LOGO: 'Brand logo',
  REFERENCE_IMAGE: 'Reference image',
  REFERENCE_VIDEO: 'Reference video',
  REFERENCE_ASSET: 'Reference asset',
  EXPORT_IMAGE: 'Export image',
  EXPORT_VIDEO: 'Export video',
  OTHER: 'Other',
};

export const ASSET_FILE_TYPE_LABELS: Record<AssetFileType, string> = {
  IMAGE: 'Image',
  VIDEO: 'Video',
  VECTOR_IMAGE: 'Vector image',
};

export const ASSET_STATUS_LABELS: Record<AssetStatus, string> = {
  AVAILABLE: 'Available',
  READY: 'Ready',
  UPLOADING: 'Uploading',
  GENERATED_METADATA_ONLY: 'Metadata only',
  FAILED: 'Failed',
  DELETED: 'Deleted',
};

export function assetCategoryLabel(category: AssetCategory): string {
  return ASSET_CATEGORY_LABELS[category];
}

export function assetFileTypeLabel(fileType: AssetFileType): string {
  return ASSET_FILE_TYPE_LABELS[fileType];
}

export function assetStatusLabel(status: AssetStatus): string {
  return ASSET_STATUS_LABELS[status];
}

export function assetStatusTone(
  status: AssetStatus,
): 'brand' | 'blue' | 'red' | 'neutral' {
  switch (status) {
    case 'READY':
    case 'AVAILABLE':
      return 'brand';
    case 'UPLOADING':
      return 'blue';
    case 'FAILED':
      return 'red';
    case 'GENERATED_METADATA_ONLY':
    case 'DELETED':
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

export function isImageAsset(asset: Pick<Asset, 'fileType'>): boolean {
  return asset.fileType === 'IMAGE' || asset.fileType === 'VECTOR_IMAGE';
}

export function isVideoAsset(asset: Pick<Asset, 'fileType'>): boolean {
  return asset.fileType === 'VIDEO';
}

export function isPreviewableAsset(asset: Pick<Asset, 'fileType'>): boolean {
  return isImageAsset(asset) || isVideoAsset(asset);
}

export function normalizeAssetTag(value: string): string {
  return value.trim().toLowerCase();
}
