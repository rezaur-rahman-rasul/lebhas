import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { FeatureLimit } from '@app/core/workspace/workspace.models';
import { BrandStore } from '@app/features/brands/brand.store';
import { ProductServiceStore } from '@app/features/product-services/product-service.store';
import { ProjectStore } from '@app/features/projects/project.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import { AssetUploadSubmitPayload } from '../../components/asset-uploader/asset-uploader';
import {
  Asset,
  AssetFilter,
  DEFAULT_ASSET_FILTERS,
  formatFileSize,
  isPreviewableAsset,
} from '../../models/asset.models';
import { AssetStore } from '../../state/asset.store';
import { AssetContextBannerComponent } from '../../components/asset-context-banner/asset-context-banner';
import { AssetFilterBar } from '../../components/asset-filter-bar/asset-filter-bar';
import { AssetGrid } from '../../components/asset-grid/asset-grid';
import { AssetList } from '../../components/asset-list/asset-list';
import { AssetPreviewDrawer } from '../../components/asset-preview-drawer/asset-preview-drawer';
import { AssetUploader } from '../../components/asset-uploader/asset-uploader';

@Component({
  selector: 'app-project-assets-page',
  standalone: true,
  imports: [
    RouterLink,
    BadgeComponent,
    ButtonComponent,
    EmptyStateComponent,
    ModalShellComponent,
    PageHeaderComponent,
    AssetContextBannerComponent,
    AssetFilterBar,
    AssetGrid,
    AssetList,
    AssetPreviewDrawer,
    AssetUploader,
  ],
  templateUrl: './project-assets.html',
  styleUrl: './project-assets.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectAssetsPage {
  protected readonly store = inject(AssetStore);
  private readonly auth = inject(CurrentUserStore);
  protected readonly workspace = inject(WorkspaceStore);
  private readonly brandStore = inject(BrandStore);
  private readonly productStore = inject(ProductServiceStore);
  private readonly projectStore = inject(ProjectStore);
  private readonly route = inject(ActivatedRoute);

  private readonly uploaderOpenSignal = signal(false);
  private readonly previewOpenSignal = signal(false);
  private readonly previewUrlSignal = signal<string | null>(null);
  private readonly previewLoadingSignal = signal(false);
  private readonly deleteDialogOpenSignal = signal(false);
  private readonly pendingDeleteAssetSignal = signal<Asset | null>(null);
  private readonly downloadingAssetIdSignal = signal<string | null>(null);
  private readonly uploadFieldErrorsSignal = signal<Readonly<Record<string, string>>>({});
  private readonly uploadErrorSignal = signal<string | null>(null);

  protected readonly uploaderOpen = this.uploaderOpenSignal.asReadonly();
  protected readonly previewOpen = this.previewOpenSignal.asReadonly();
  protected readonly previewUrl = this.previewUrlSignal.asReadonly();
  protected readonly previewLoading = this.previewLoadingSignal.asReadonly();
  protected readonly deleteDialogOpen = this.deleteDialogOpenSignal.asReadonly();
  protected readonly pendingDeleteAsset = this.pendingDeleteAssetSignal.asReadonly();
  protected readonly downloadingAssetId = this.downloadingAssetIdSignal.asReadonly();
  protected readonly uploadFieldErrors = this.uploadFieldErrorsSignal.asReadonly();
  protected readonly uploadError = this.uploadErrorSignal.asReadonly();

  protected readonly projectId = computed(() => this.route.snapshot.paramMap.get('projectId') ?? '');
  protected readonly project = computed(
    () => this.projectStore.items().find((item) => item.id === this.projectId()) ?? null,
  );
  protected readonly product = computed(() => {
    const project = this.project();
    return this.productStore.items().find((item) => item.id === project?.productServiceId) ?? null;
  });
  protected readonly brand = computed(() => {
    const product = this.product();
    return this.brandStore.items().find((item) => item.id === product?.brandId) ?? null;
  });
  protected readonly hasWorkspaceContext = computed(() => Boolean(this.auth.activeWorkspaceId()));
  protected readonly assetCountLabel = computed(
    () => `${this.store.pagination().totalItems} asset${this.store.pagination().totalItems === 1 ? '' : 's'}`,
  );
  protected readonly storageSummary = computed(() => {
    const storageLimit = this.storageLimit();
    const remainingBytes = this.workspace.usage()?.storageRemainingBytes;

    if (!storageLimit && typeof remainingBytes !== 'number') {
      return null;
    }

    const unit = storageLimit?.unit ?? 'bytes';
    const used = storageLimit?.used ?? null;
    const limit = storageLimit?.limit ?? null;
    const remaining = storageLimit?.remaining ?? remainingBytes ?? null;
    const percentage =
      typeof used === 'number' && typeof limit === 'number' && limit > 0
        ? Math.min(100, Math.max(0, Math.round((used / limit) * 100)))
        : null;

    return {
      usedLabel: this.formatLimitValue(used, unit),
      limitLabel: this.formatLimitValue(limit, unit),
      remainingLabel: this.formatLimitValue(remaining, unit),
      percentage,
      message: storageLimit?.message ?? null,
    };
  });
  protected readonly skeletonItems = Array.from({ length: 6 }, (_, index) => index);

  constructor() {
    void this.initialize();
  }

  protected async initialize(): Promise<void> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId || !this.projectId()) {
      return;
    }

    await Promise.all([
      this.brandStore.load(workspaceId),
      this.productStore.load(workspaceId),
      this.projectStore.load(workspaceId),
    ]);

    await this.store.loadProjectAssets(this.projectId());
  }

  protected openUploader(): void {
    this.uploadFieldErrorsSignal.set({});
    this.uploadErrorSignal.set(null);
    this.uploaderOpenSignal.set(true);
  }

  protected closeUploader(): void {
    this.uploadFieldErrorsSignal.set({});
    this.uploadErrorSignal.set(null);
    this.uploaderOpenSignal.set(false);
  }

  protected async submitUpload(payload: AssetUploadSubmitPayload): Promise<void> {
    this.uploadFieldErrorsSignal.set({});
    this.uploadErrorSignal.set(null);
    const result = await this.store.uploadAsset({
      ...payload,
      projectId: this.projectId(),
    });

    if (result.ok) {
      this.closeUploader();
      await this.store.loadProjectAssets(this.projectId());
    } else {
      this.uploadFieldErrorsSignal.set(result.fieldErrors);
      this.uploadErrorSignal.set(result.message ?? null);
    }
  }

  protected cancelUpload(): void {
    this.store.cancelUpload();
    this.closeUploader();
  }

  protected async applyFilters(filters: AssetFilter): Promise<void> {
    await this.store.applyFilters(filters);
  }

  protected async resetFilters(): Promise<void> {
    await this.store.applyFilters(DEFAULT_ASSET_FILTERS);
  }

  protected async openPreview(asset: Asset): Promise<void> {
    this.store.selectAsset(asset);
    this.previewOpenSignal.set(true);
    this.previewUrlSignal.set(null);

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
    const preview = await this.store.getPreviewUrl(asset.id);
    if (preview?.url) {
      this.previewUrlSignal.set(preview.url);
    }
    this.previewLoadingSignal.set(false);
  }

  protected async downloadAsset(asset: Asset): Promise<void> {
    this.downloadingAssetIdSignal.set(asset.id);
    const downloadUrl = await this.store.getDownloadUrl(asset.id);
    this.downloadingAssetIdSignal.set(null);

    if (downloadUrl?.url) {
      window.open(downloadUrl.url, '_blank', 'noopener,noreferrer');
    }
  }

  protected openAssetDetail(asset: Asset): void {
    void this.openPreview(asset);
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

    const result = await this.store.deleteAsset(asset.id);
    if (result.ok) {
      if (this.store.selectedAsset()?.id === asset.id) {
        this.closePreview();
      }
      this.cancelDelete();
    }
  }

  protected goToPreviousPage(): void {
    void this.store.goToPage(this.store.pagination().page - 1);
  }

  protected goToNextPage(): void {
    void this.store.goToPage(this.store.pagination().page + 1);
  }

  protected reloadAssets(): void {
    void this.store.loadProjectAssets(this.projectId());
  }

  private storageLimit(): FeatureLimit | null {
    return (
      this.workspace.featureLimit('assets.storage') ??
      this.workspace.featureLimit('asset.storage') ??
      this.workspace.featureLimit('storage')
    );
  }

  private formatLimitValue(value: number | null | undefined, unit: string | null | undefined): string {
    if (typeof value !== 'number') {
      return 'Unavailable';
    }

    return unit === 'bytes' || !unit ? formatFileSize(value) : `${value} ${unit}`;
  }
}
