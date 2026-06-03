import {
  AssetCategory,
  MAX_DESCRIPTION_LENGTH,
  MAX_DISPLAY_NAME_LENGTH,
  MAX_IMAGE_BYTES,
  MAX_LOGO_BYTES,
  MAX_TAG_LENGTH,
  MAX_VIDEO_BYTES,
  UploadAssetPayload,
} from '../models/asset.models';

const IMAGE_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'webp']);
const VIDEO_EXTENSIONS = new Set(['mp4', 'mov']);
const LOGO_EXTENSIONS = new Set(['svg', 'png', 'webp', 'jpg', 'jpeg']);

export function validateUploadPayload(
  payload: UploadAssetPayload,
  workspaceId: string | null,
): Readonly<Record<string, string>> {
  const errors: Record<string, string> = {};

  if (!workspaceId) {
    errors['workspaceId'] = 'Select a workspace before uploading assets.';
  }

  if (!payload.projectId?.trim()) {
    errors['projectId'] = 'Select a project before uploading assets.';
  }

  if (!payload.file) {
    errors['file'] = 'Choose a file to upload.';
  } else if (payload.file.size <= 0) {
    errors['file'] = 'The selected file is empty.';
  } else {
    const extension = extensionOf(payload.file.name);
    const limit = maxBytesFor(payload.assetCategory, extension);

    if (!isSupportedExtension(payload.assetCategory, extension)) {
      errors['file'] = 'This file type is not supported for the selected category.';
    } else if (payload.file.size > limit) {
      errors['file'] = `File exceeds the maximum size of ${formatLimit(limit)}.`;
    }
  }

  if (!payload.displayName.trim()) {
    errors['displayName'] = 'Enter a display name.';
  } else if (payload.displayName.trim().length > MAX_DISPLAY_NAME_LENGTH) {
    errors['displayName'] = `Display name must be ${MAX_DISPLAY_NAME_LENGTH} characters or fewer.`;
  }

  if (payload.description.length > MAX_DESCRIPTION_LENGTH) {
    errors['description'] = `Description must be ${MAX_DESCRIPTION_LENGTH} characters or fewer.`;
  }

  for (const tag of payload.tags) {
    if (tag.length > MAX_TAG_LENGTH) {
      errors['tags'] = `Each tag must be ${MAX_TAG_LENGTH} characters or fewer.`;
      break;
    }
  }

  return errors;
}

function extensionOf(fileName: string): string {
  const parts = fileName.split('.');
  return parts.length > 1 ? parts[parts.length - 1]!.toLowerCase() : '';
}

function isSupportedExtension(category: AssetCategory, extension: string): boolean {
  if (!extension) {
    return false;
  }

  if (category === 'BRAND_LOGO') {
    return LOGO_EXTENSIONS.has(extension);
  }

  if (category === 'PRODUCT_VIDEO' || category === 'REFERENCE_VIDEO' || category === 'EXPORT_VIDEO') {
    return VIDEO_EXTENSIONS.has(extension);
  }

  return IMAGE_EXTENSIONS.has(extension);
}

function maxBytesFor(category: AssetCategory, extension: string): number {
  if (category === 'BRAND_LOGO' || extension === 'svg') {
    return MAX_LOGO_BYTES;
  }

  if (category === 'PRODUCT_VIDEO' || category === 'REFERENCE_VIDEO' || category === 'EXPORT_VIDEO' || VIDEO_EXTENSIONS.has(extension)) {
    return MAX_VIDEO_BYTES;
  }

  return MAX_IMAGE_BYTES;
}

function formatLimit(bytes: number): string {
  if (bytes >= 1024 * 1024) {
    return `${Math.round(bytes / (1024 * 1024))} MB`;
  }

  return `${Math.round(bytes / 1024)} KB`;
}
