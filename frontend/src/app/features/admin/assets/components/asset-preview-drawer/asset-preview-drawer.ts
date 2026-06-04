import { DatePipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  Asset,
  assetCategoryLabel,
  assetFileTypeLabel,
  assetStatusLabel,
  assetStatusTone,
  formatFileSize,
} from '../../models/asset.models';

@Component({
  selector: 'app-asset-preview-drawer',
  standalone: true,
  imports: [DatePipe, KeyValuePipe, BadgeComponent, ButtonComponent, IconComponent],
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
  protected readonly resolvedPreviewUrl = computed(() => {
    const asset = this.asset();
    return normalizePreviewUrl(this.previewUrl(), asset);
  });
  protected readonly canRenderPreview = computed(() => Boolean(this.resolvedPreviewUrl()) && !this.previewLoadFailed());

  protected readonly assetCategoryLabel = assetCategoryLabel;
  protected readonly assetFileTypeLabel = assetFileTypeLabel;
  protected readonly assetStatusLabel = assetStatusLabel;
  protected readonly assetStatusTone = assetStatusTone;
  protected readonly formatFileSize = formatFileSize;

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
    return asset.width && asset.height ? `${asset.width} x ${asset.height}` : 'Not detected';
  }

  protected durationLabel(asset: Asset): string {
    if (asset.fileType !== 'VIDEO') {
      return 'Not applicable';
    }
    return asset.duration ? `${asset.duration}s` : 'Not detected';
  }

  protected storageProviderLabel(asset: Asset): string {
    return asset.storageProvider === 'R2' ? 'Cloudflare R2' : asset.storageProvider;
  }

  protected visibilityLabel(asset: Asset): string {
    return asset.publicUrl ? 'Public URL' : 'Private / signed URL';
  }
}

function normalizePreviewUrl(inputUrl: string | null, asset: Asset | null): string | null {
  return (
    cleanUrl(inputUrl) ||
    readUrlField(asset, 'signedPreviewUrl') ||
    cleanUrl(asset?.previewUrl) ||
    readUrlField(asset, 'publicPreviewUrl') ||
    cleanUrl(asset?.thumbnailUrl) ||
    cleanUrl(asset?.publicUrl) ||
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
