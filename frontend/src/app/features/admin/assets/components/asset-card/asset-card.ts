import { DatePipe } from '@angular/common';
import { HttpContext } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  Asset,
  assetCategoryLabel,
  assetFileTypeLabel,
  assetStatusLabel,
  assetStatusTone,
  formatFileSize,
  isPreviewableAsset,
} from '../../models/asset.models';
import { AssetService } from '../../services/asset.service';

const cardPreviewUrlCache = new Map<string, string>();

@Component({
  selector: 'app-asset-card',
  standalone: true,
  imports: [DatePipe, BadgeComponent, ButtonComponent, CardComponent, IconComponent],
  templateUrl: './asset-card.html',
  styleUrl: './asset-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetCard {
  private readonly assetService = inject(AssetService);
  private readonly auth = inject(CurrentUserStore);

  readonly asset = input.required<Asset>();
  readonly canDownload = input(false);
  readonly canDelete = input(false);
  readonly deleting = input(false);

  readonly previewRequested = output<Asset>();
  readonly downloadRequested = output<Asset>();
  readonly deleteRequested = output<Asset>();
  readonly detailRequested = output<Asset>();

  protected readonly resolvedPreviewUrl = computed(() => {
    const asset = this.asset();
    const failedUrl = this.failedPreviewUrl();
    return [
      cleanUrl(asset.thumbnailUrl),
      cleanUrl(asset.previewUrl),
      cleanUrl(asset.publicUrl),
      this.signedPreviewUrl(),
    ].find((url): url is string => Boolean(url && url !== failedUrl)) ?? null;
  });
  protected readonly previewLoadFailed = signal(false);
  private readonly signedPreviewUrl = signal<string | null>(null);
  private readonly failedPreviewUrl = signal<string | null>(null);
  private readonly signedPreviewRequested = signal(false);
  private currentAssetId: string | null = null;

  protected readonly assetCategoryLabel = assetCategoryLabel;
  protected readonly assetFileTypeLabel = assetFileTypeLabel;
  protected readonly assetStatusLabel = assetStatusLabel;
  protected readonly assetStatusTone = assetStatusTone;
  protected readonly formatFileSize = formatFileSize;

  constructor() {
    effect(() => {
      const asset = this.asset();
      if (this.currentAssetId !== asset.id) {
        this.currentAssetId = asset.id;
        this.previewLoadFailed.set(false);
        this.failedPreviewUrl.set(null);
        this.signedPreviewRequested.set(false);
        this.signedPreviewUrl.set(cardPreviewUrlCache.get(asset.id) ?? null);
      }

      if (
        !this.resolvedPreviewUrl() &&
        isPreviewableAsset(asset) &&
        !this.signedPreviewRequested()
      ) {
        void this.loadSignedPreview(asset);
      }
    });
  }

  protected markPreviewFailed(): void {
    const asset = this.asset();
    const failedUrl = this.resolvedPreviewUrl();
    if (failedUrl) {
      this.failedPreviewUrl.set(failedUrl);
    }

    cardPreviewUrlCache.delete(asset.id);
    this.signedPreviewUrl.set(null);

    if (isPreviewableAsset(asset) && !this.signedPreviewRequested()) {
      void this.loadSignedPreview(asset, true);
      return;
    }

    this.previewLoadFailed.set(true);
  }

  protected markPreviewLoaded(): void {
    this.previewLoadFailed.set(false);
    this.failedPreviewUrl.set(null);
  }

  private async loadSignedPreview(asset: Asset, force = false): Promise<void> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId || (!force && cardPreviewUrlCache.has(asset.id))) {
      return;
    }

    this.signedPreviewRequested.set(true);

    try {
      const preview = await firstValueFrom(
        this.assetService.getPreviewUrl(
          workspaceId,
          asset.id,
          new HttpContext().set(SKIP_ERROR_TOAST, true),
        ),
      );
      if (preview.url && preview.url !== this.failedPreviewUrl()) {
        cardPreviewUrlCache.set(asset.id, preview.url);
        if (this.asset().id === asset.id) {
          this.signedPreviewUrl.set(preview.url);
        }
      } else if (this.asset().id === asset.id) {
        this.previewLoadFailed.set(true);
      }
    } catch {
      if (this.asset().id === asset.id) {
        this.previewLoadFailed.set(true);
      }
    }
  }
}

function cleanUrl(value: string | null | undefined): string | null {
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}
