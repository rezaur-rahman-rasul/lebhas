import { DatePipe } from '@angular/common';
import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';

import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  Asset,
  assetCategoryLabel,
  assetFileExtension,
  assetStatusLabel,
  assetStatusTone,
  assetTypeLabel,
  formatFileSize,
  isVideoAsset,
  isPreviewableAsset,
  resolveFileSize,
} from '../../models/asset.models';
import { AssetApiService } from '../../services/asset-api.service';

const cardPreviewUrlCache = new Map<string, string>();
const failedSignedPreviewRequests = new Set<string>();

@Component({
  selector: 'app-asset-card',
  standalone: true,
  imports: [DatePipe, BadgeComponent, ButtonComponent, CardComponent, IconComponent],
  templateUrl: './asset-card.html',
  styleUrl: './asset-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetCard {
  private readonly assetService = inject(AssetApiService);
  private readonly auth = inject(CurrentUserStore);

  readonly asset = input.required<Asset>();
  readonly canDownload = input(false);
  readonly canDelete = input(false);
  readonly deleting = input(false);

  readonly previewRequested = output<Asset>();
  readonly downloadRequested = output<Asset>();
  readonly deleteRequested = output<Asset>();
  readonly detailRequested = output<Asset>();

  protected readonly resolvedPreviewUrl = computed(() => resolveAssetPreviewUrl(this.asset(), this.signedPreviewUrl()));
  protected readonly previewLoadFailed = signal(false);
  private readonly signedPreviewUrl = signal<string | null>(null);

  protected readonly assetCategoryLabel = assetCategoryLabel;
  protected readonly assetTypeLabel = assetTypeLabel;
  protected readonly assetStatusLabel = assetStatusLabel;
  protected readonly assetStatusTone = assetStatusTone;
  protected readonly formatFileSize = formatFileSize;
  protected readonly resolveFileSize = resolveFileSize;
  protected readonly assetFileExtension = assetFileExtension;
  protected readonly isVideoAsset = isVideoAsset;

  constructor() {
    effect(() => {
      const asset = this.asset();
      this.previewLoadFailed.set(false);
      this.signedPreviewUrl.set(cardPreviewUrlCache.get(asset.id) ?? null);

      if (!this.resolvedPreviewUrl() && isPreviewableAsset(asset)) {
        void this.loadSignedPreview(asset);
      }
    });
  }

  protected markPreviewFailed(): void {
    const asset = this.asset();
    this.previewLoadFailed.set(true);
    if (isPreviewableAsset(asset)) {
      void this.loadSignedPreview(asset);
    }
  }

  protected markPreviewLoaded(): void {
    this.previewLoadFailed.set(false);
  }

  private async loadSignedPreview(asset: Asset): Promise<void> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId || cardPreviewUrlCache.has(asset.id) || failedSignedPreviewRequests.has(asset.id)) {
      return;
    }

    try {
      const preview = await this.assetService.getPreviewUrl(
        workspaceId,
        asset.id,
        new HttpContext().set(SKIP_ERROR_TOAST, true),
      );

      if (preview.url) {
        cardPreviewUrlCache.set(asset.id, preview.url);
        if (this.asset().id === asset.id) {
          this.signedPreviewUrl.set(preview.url);
        }
      }
    } catch {
      failedSignedPreviewRequests.add(asset.id);
      if (this.asset().id === asset.id) {
        this.previewLoadFailed.set(true);
      }
    }
  }
}

function resolveAssetPreviewUrl(asset: Asset, signedPreviewUrl: string | null): string | null {
  return (
    cleanUrl(asset.thumbnailUrl) ||
    cleanUrl(asset.previewUrl) ||
    cleanUrl(asset.publicPreviewUrl) ||
    cleanUrl(asset.publicUrl) ||
    cleanUrl(asset.signedPreviewUrl) ||
    cleanUrl(signedPreviewUrl) ||
    cleanUrl(asset.url) ||
    cleanUrl(asset.downloadUrl)
  );
}

function cleanUrl(value: string | null | undefined): string | null {
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}
