import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { DatePipe, KeyValuePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  Asset,
  assetCategoryLabel,
  assetFileTypeLabel,
  assetStatusLabel,
  assetStatusTone,
  formatFileSize,
  isPreviewableAsset,
} from '../../models/asset.models';
import { AssetStore } from '../../state/asset.store';
import { AssetTagEditor } from '../../components/asset-tag-editor/asset-tag-editor';

@Component({
  selector: 'app-asset-detail-page',
  standalone: true,
  imports: [
    DatePipe,
    KeyValuePipe,
    RouterLink,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    ModalShellComponent,
    PageHeaderComponent,
    AssetTagEditor,
  ],
  templateUrl: './asset-detail.html',
  styleUrl: './asset-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetDetailPage {
  protected readonly store = inject(AssetStore);
  private readonly auth = inject(CurrentUserStore);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly assetId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('assetId'))),
    { initialValue: this.route.snapshot.paramMap.get('assetId') },
  );

  private readonly previewUrlSignal = signal<string | null>(null);
  private readonly previewLoadingSignal = signal(false);
  private readonly downloadLoadingSignal = signal(false);
  private readonly deleteDialogOpenSignal = signal(false);

  protected readonly previewUrl = this.previewUrlSignal.asReadonly();
  protected readonly previewLoading = this.previewLoadingSignal.asReadonly();
  protected readonly downloadLoading = this.downloadLoadingSignal.asReadonly();
  protected readonly deleteDialogOpen = this.deleteDialogOpenSignal.asReadonly();

  protected readonly asset = this.store.selectedAsset;
  protected readonly hasWorkspaceContext = computed(() => Boolean(this.auth.activeWorkspaceId()));
  protected readonly roleLabel = computed(() => this.auth.currentRole() ?? 'ADMIN');
  protected readonly roleTone = computed(() =>
    this.auth.currentRole() === 'MASTER'
      ? 'red'
      : this.auth.currentRole() === 'CREW'
        ? 'blue'
        : 'brand',
  );
  protected readonly assetCategoryLabel = assetCategoryLabel;
  protected readonly assetFileTypeLabel = assetFileTypeLabel;
  protected readonly assetStatusLabel = assetStatusLabel;
  protected readonly assetStatusTone = assetStatusTone;
  protected readonly formatFileSize = formatFileSize;

  constructor() {
    effect(() => {
      const assetId = this.assetId();
      if (assetId) {
        void this.loadAsset(assetId);
      }
    });
  }

  protected async refreshPreview(): Promise<void> {
    const asset = this.asset();
    if (!asset || !isPreviewableAsset(asset)) {
      return;
    }

    this.previewLoadingSignal.set(true);
    const preview = await this.store.getPreviewUrl(asset.id);
    if (preview?.url) {
      this.previewUrlSignal.set(preview.url);
    }
    this.previewLoadingSignal.set(false);
  }

  protected async downloadAsset(): Promise<void> {
    const asset = this.asset();
    if (!asset) {
      return;
    }

    this.downloadLoadingSignal.set(true);
    const downloadUrl = await this.store.getDownloadUrl(asset.id);
    this.downloadLoadingSignal.set(false);

    if (downloadUrl?.url) {
      window.open(downloadUrl.url, '_blank', 'noopener,noreferrer');
    }
  }

  protected openDeleteDialog(): void {
    this.deleteDialogOpenSignal.set(true);
  }

  protected closeDeleteDialog(): void {
    this.deleteDialogOpenSignal.set(false);
  }

  protected async deleteAsset(): Promise<void> {
    const asset = this.asset();
    if (!asset) {
      return;
    }

    const result = await this.store.deleteAsset(asset.id);
    if (result.ok) {
      this.closeDeleteDialog();
      void this.router.navigate(['../'], { relativeTo: this.route });
    }
  }

  protected async saveTags(tags: readonly string[]): Promise<void> {
    const asset = this.asset();
    if (!asset) {
      return;
    }

    await this.store.updateAsset(asset.id, { tags }, 'Asset tags updated');
  }

  private async loadAsset(assetId: string): Promise<void> {
    await this.store.loadAssetDetail(assetId);
    const asset = this.asset();
    if (!asset) {
      return;
    }

    this.previewUrlSignal.set(asset.thumbnailUrl || asset.previewUrl || asset.publicUrl);
    if (isPreviewableAsset(asset)) {
      await this.refreshPreview();
    }
  }
}
