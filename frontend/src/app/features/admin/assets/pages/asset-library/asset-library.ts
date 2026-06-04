import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  Asset,
  AssetFilter,
  CreateAssetFolderPayload,
  DEFAULT_ASSET_FILTERS,
  isPreviewableAsset,
  UpdateAssetFolderPayload,
  UploadAssetPayload,
} from '../../models/asset.models';
import { AssetStore } from '../../state/asset.store';
import { AssetFilterBar } from '../../components/asset-filter-bar/asset-filter-bar';
import { AssetFolderPanel } from '../../components/asset-folder-panel/asset-folder-panel';
import { AssetGrid } from '../../components/asset-grid/asset-grid';
import { AssetList } from '../../components/asset-list/asset-list';
import { AssetPreviewDrawer } from '../../components/asset-preview-drawer/asset-preview-drawer';
import { AssetUploader } from '../../components/asset-uploader/asset-uploader';

@Component({
  selector: 'app-asset-library-page',
  standalone: true,
  imports: [
    BadgeComponent,
    ButtonComponent,
    EmptyStateComponent,
    ModalShellComponent,
    PageHeaderComponent,
    AssetFilterBar,
    AssetFolderPanel,
    AssetGrid,
    AssetList,
    AssetPreviewDrawer,
    AssetUploader,
  ],
  templateUrl: './asset-library.html',
  styleUrl: './asset-library.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetLibraryPage {
  protected readonly store = inject(AssetStore);
  private readonly auth = inject(CurrentUserStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly uploaderOpenSignal = signal(false);
  private readonly previewOpenSignal = signal(false);
  private readonly previewUrlSignal = signal<string | null>(null);
  private readonly previewLoadingSignal = signal(false);
  private readonly deleteDialogOpenSignal = signal(false);
  private readonly pendingDeleteAssetSignal = signal<Asset | null>(null);
  private readonly downloadingAssetIdSignal = signal<string | null>(null);
  private readonly uploadFieldErrorsSignal = signal<Readonly<Record<string, string>>>({});
  private readonly uploadFormErrorSignal = signal<string | null>(null);

  protected readonly uploaderOpen = this.uploaderOpenSignal.asReadonly();
  protected readonly previewOpen = this.previewOpenSignal.asReadonly();
  protected readonly previewUrl = this.previewUrlSignal.asReadonly();
  protected readonly previewLoading = this.previewLoadingSignal.asReadonly();
  protected readonly deleteDialogOpen = this.deleteDialogOpenSignal.asReadonly();
  protected readonly pendingDeleteAsset = this.pendingDeleteAssetSignal.asReadonly();
  protected readonly downloadingAssetId = this.downloadingAssetIdSignal.asReadonly();
  protected readonly uploadFieldErrors = this.uploadFieldErrorsSignal.asReadonly();
  protected readonly uploadFormError = this.uploadFormErrorSignal.asReadonly();

  protected readonly hasWorkspaceContext = computed(() => Boolean(this.auth.activeWorkspaceId()));
  protected readonly roleLabel = computed(() => this.auth.currentRole() ?? 'ADMIN');
  protected readonly roleTone = computed(() =>
    this.auth.currentRole() === 'MASTER'
      ? 'red'
      : this.auth.currentRole() === 'CREW'
        ? 'blue'
        : 'brand',
  );
  protected readonly assetCountLabel = computed(
    () => `${this.store.realAssets().length} asset${this.store.realAssets().length === 1 ? '' : 's'}`,
  );
  protected readonly hasActiveFilters = computed(() => {
    const filters = this.store.filters();
    return Boolean(
      filters.search.trim() ||
        filters.assetCategory ||
        filters.fileType ||
        filters.status ||
        filters.uploadedBy ||
        filters.tag.trim() ||
        filters.createdFrom ||
        filters.createdTo ||
        this.store.selectedFolder() !== 'all',
    );
  });
  protected readonly description = computed(
    () => 'Organize source files, logos, and campaign-ready media inside the current workspace.',
  );
  protected readonly skeletonItems = Array.from({ length: 6 }, (_, index) => index);

  constructor() {
    void this.store.loadLibraryContext();
  }

  protected openUploader(): void {
    this.uploadFieldErrorsSignal.set({});
    this.uploadFormErrorSignal.set(null);
    this.uploaderOpenSignal.set(true);
  }

  protected closeUploader(): void {
    this.uploadFieldErrorsSignal.set({});
    this.uploadFormErrorSignal.set(null);
    this.uploaderOpenSignal.set(false);
  }

  protected async submitUpload(payload: UploadAssetPayload): Promise<void> {
    this.uploadFieldErrorsSignal.set({});
    this.uploadFormErrorSignal.set(null);
    const result = await this.store.uploadAsset(payload);

    if (result.ok) {
      this.closeUploader();
      return;
    }

    this.uploadFieldErrorsSignal.set(result.fieldErrors);
    this.uploadFormErrorSignal.set(result.message ?? 'Image upload failed. Check the file and try again.');
  }

  protected cancelUpload(): void {
    this.store.cancelUpload();
    this.closeUploader();
  }

  protected async applyFilters(filters: AssetFilter): Promise<void> {
    await this.store.applyFilters(filters);
  }

  protected async resetFilters(): Promise<void> {
    await this.store.applyFilters({
      ...DEFAULT_ASSET_FILTERS,
      folderId: this.store.filters().folderId,
    });
  }

  protected async selectFolder(folder: string): Promise<void> {
    await this.store.selectFolder(folder);
  }

  protected async openPreview(asset: Asset): Promise<void> {
    this.store.selectAsset(asset);
    this.previewOpenSignal.set(true);
    this.previewUrlSignal.set(getAssetPreviewUrl(asset));

    if (isPreviewableAsset(asset)) {
      await this.refreshPreview(asset);
    }
  }

  protected closePreview(): void {
    this.previewOpenSignal.set(false);
    this.previewUrlSignal.set(null);
    this.store.selectAsset(null);
  }

  protected async refreshPreview(asset: Asset): Promise<void> {
    this.previewLoadingSignal.set(true);
    try {
      const preview = await this.store.getPreviewUrl(asset.id);
      if (preview?.url) {
        this.previewUrlSignal.set(preview.url);
        this.store.selectAsset({ ...asset, previewUrl: preview.url });
      }
    } finally {
      this.previewLoadingSignal.set(false);
    }
  }

  protected async downloadAsset(asset: Asset): Promise<void> {
    this.downloadingAssetIdSignal.set(asset.id);
    try {
      const downloadUrl = await this.store.getDownloadUrl(asset.id);
      if (downloadUrl?.url) {
        window.open(downloadUrl.url, '_blank', 'noopener,noreferrer');
      }
    } finally {
      this.downloadingAssetIdSignal.set(null);
    }
  }

  protected openAssetDetail(asset: Asset): void {
    void this.router.navigate([asset.id], { relativeTo: this.route });
  }

  protected confirmDelete(asset: Asset): void {
    this.pendingDeleteAssetSignal.set(asset);
    this.deleteDialogOpenSignal.set(true);
  }

  protected cancelDelete(): void {
    this.deleteDialogOpenSignal.set(false);
    this.pendingDeleteAssetSignal.set(null);
  }

  protected async deleteAsset(): Promise<void> {
    const asset = this.pendingDeleteAssetSignal();
    if (!asset) {
      return;
    }

    if (this.store.selectedAsset()?.id === asset.id) {
      this.closePreview();
    }

    this.cancelDelete();
    await this.store.deleteAsset(asset.id);
  }

  protected async createFolder(payload: CreateAssetFolderPayload): Promise<void> {
    await this.store.createFolder(payload);
  }

  protected async renameFolder(event: {
    readonly folderId: string;
    readonly payload: UpdateAssetFolderPayload;
  }): Promise<void> {
    await this.store.renameFolder(event.folderId, event.payload);
  }

  protected async deleteFolder(folderId: string): Promise<void> {
    await this.store.deleteFolder(folderId);
  }

  protected goToPreviousPage(): void {
    void this.store.goToPage(this.store.pagination().page - 1);
  }

  protected goToNextPage(): void {
    void this.store.goToPage(this.store.pagination().page + 1);
  }

  protected reloadLibrary(): void {
    void this.store.loadLibraryContext();
  }
}

function getAssetPreviewUrl(asset: Asset): string | null {
  return (
    readUrl(asset, 'signedPreviewUrl') ||
    asset.previewUrl ||
    readUrl(asset, 'publicPreviewUrl') ||
    asset.thumbnailUrl ||
    asset.publicUrl ||
    readUrl(asset, 'url') ||
    readUrl(asset, 'downloadUrl') ||
    null
  );
}

function readUrl(asset: Asset, key: string): string | null {
  const value = (asset as unknown as Record<string, unknown>)[key];
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}
