import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  Asset,
  assetCategoryLabel,
  assetFileExtension,
  assetTypeLabel,
  resolveFileSize,
  assetStatusLabel,
  assetStatusTone,
  formatFileSize,
  isVideoAsset,
} from '../../models/asset.models';

@Component({
  selector: 'app-asset-preview-drawer',
  standalone: true,
  imports: [DatePipe, BadgeComponent, ButtonComponent, IconComponent],
  templateUrl: './asset-preview-drawer.html',
  styleUrl: './asset-preview-drawer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetPreviewDrawer {
  readonly open = input(false);
  readonly asset = input<Asset | null>(null);
  readonly previewUrl = input<string | null>(null);
  readonly previewLoading = input(false);
  readonly downloading = input(false);
  readonly deleting = input(false);
  readonly canDownload = input(false);
  readonly canDelete = input(false);

  readonly closed = output<void>();
  readonly previewRequested = output<Asset>();
  readonly downloadRequested = output<Asset>();
  readonly deleteRequested = output<Asset>();
  readonly detailRequested = output<Asset>();

  protected readonly previewLoadFailed = signal(false);
  protected readonly resolvedPreviewUrl = computed(() => normalizePreviewUrl(this.previewUrl(), this.asset()));
  protected readonly canRenderPreview = computed(() => Boolean(this.resolvedPreviewUrl()) && !this.previewLoadFailed());

  protected readonly assetCategoryLabel = assetCategoryLabel;
  protected readonly assetTypeLabel = assetTypeLabel;
  protected readonly assetFileExtension = assetFileExtension;
  protected readonly resolveFileSize = resolveFileSize;
  protected readonly assetStatusLabel = assetStatusLabel;
  protected readonly assetStatusTone = assetStatusTone;
  protected readonly formatFileSize = formatFileSize;
  protected readonly isVideoAsset = isVideoAsset;

  constructor() {
    effect(() => {
      this.asset()?.id;
      this.previewUrl();
      this.previewLoadFailed.set(false);
    });
  }

  protected refreshPreview(asset: Asset): void {
    this.previewLoadFailed.set(false);
    this.previewRequested.emit(asset);
  }

  protected markPreviewLoaded(): void {
    this.previewLoadFailed.set(false);
  }

  protected markPreviewFailed(): void {
    this.previewLoadFailed.set(true);
  }

  protected dimensionsLabel(asset: Asset): string {
    const width = asset.storageFile?.width;
    const height = asset.storageFile?.height;
    return width && height ? `${width} x ${height}` : 'Not detected';
  }

  protected durationLabel(asset: Asset): string {
    if (!isVideoAsset(asset)) {
      return 'Not applicable';
    }
    return asset.storageFile?.duration ? `${asset.storageFile.duration}s` : 'Not detected';
  }

  protected storageProviderLabel(asset: Asset): string {
    const provider = asset.storageFile?.provider;
    return provider === 'R2' ? 'Cloudflare R2' : provider || 'Private storage';
  }

  protected visibilityLabel(asset: Asset): string {
    return readUrlField(asset, 'publicUrl') ? 'Public URL' : 'Private / signed URL';
  }

  protected contentTypeLabel(asset: Asset): string {
    return asset.storageFile?.mimeType || 'Unknown';
  }
}

function normalizePreviewUrl(inputUrl: string | null, asset: Asset | null): string | null {
  return (
    cleanUrl(inputUrl) ||
    readUrlField(asset, 'signedPreviewUrl') ||
    cleanUrl(asset?.previewUrl) ||
    readUrlField(asset, 'publicPreviewUrl') ||
    cleanUrl(asset?.thumbnailUrl) ||
    readUrlField(asset, 'publicUrl') ||
    readUrlField(asset, 'url') ||
    readUrlField(asset, 'downloadUrl') ||
    readNestedUrl(asset, 'data', 'previewUrl') ||
    null
  );
}

function cleanUrl(value: unknown): string | null {
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}

function readUrlField(asset: Asset | null, key: string): string | null {
  if (!asset) {
    return null;
  }
  return cleanUrl((asset as unknown as Record<string, unknown>)[key]);
}

function readNestedUrl(asset: Asset | null, parentKey: string, childKey: string): string | null {
  if (!asset) {
    return null;
  }
  const parent = (asset as unknown as Record<string, unknown>)[parentKey];
  if (!parent || typeof parent !== 'object') {
    return null;
  }
  return cleanUrl((parent as Record<string, unknown>)[childKey]);
}
