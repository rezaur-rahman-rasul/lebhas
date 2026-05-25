import { HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom, Subscription } from 'rxjs';

import { normalizeHttpError } from '@app/core/api/http-error';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { Permission } from '@app/features/auth/models/user.models';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  Asset,
  AssetActionResult,
  AssetFilter,
  AssetPagination,
  AssetViewMode,
  DEFAULT_ASSET_FILTERS,
  DEFAULT_ASSET_PAGINATION,
  UpdateAssetPayload,
  UploadAssetPayload,
  UploadState,
} from '../models/asset.models';
import { AssetApiService } from '../services/asset-api.service';
import { validateUploadPayload } from '../services/asset.validation';

@Injectable({ providedIn: 'root' })
export class AssetStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly permissions = inject(PermissionStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly assetService = inject(AssetApiService);

  private readonly assetsSignal = signal<readonly Asset[]>([]);
  private readonly selectedAssetSignal = signal<Asset | null>(null);
  private readonly selectedProjectIdSignal = signal<string | null>(null);
  private readonly filtersSignal = signal<AssetFilter>(DEFAULT_ASSET_FILTERS);
  private readonly paginationSignal = signal<AssetPagination>(DEFAULT_ASSET_PAGINATION);
  private readonly viewModeSignal = signal<AssetViewMode>('grid');
  private readonly uploadProgressSignal = signal<number | null>(null);
  private readonly uploadStateSignal = signal<UploadState>('idle');
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  private activeUploadSubscription: Subscription | null = null;

  readonly assets = this.assetsSignal.asReadonly();
  readonly selectedAsset = this.selectedAssetSignal.asReadonly();
  readonly selectedProjectId = this.selectedProjectIdSignal.asReadonly();
  readonly filters = this.filtersSignal.asReadonly();
  readonly pagination = this.paginationSignal.asReadonly();
  readonly viewMode = this.viewModeSignal.asReadonly();
  readonly uploadProgress = this.uploadProgressSignal.asReadonly();
  readonly uploadState = this.uploadStateSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly assetError = this.errorSignal.asReadonly();
  readonly assetLoading = this.loadingSignal.asReadonly();

  readonly hasAssets = computed(() => this.filteredAssets().length > 0);
  readonly filteredAssets = computed(() => this.assetsSignal());
  readonly isUploading = computed(() => this.uploadStateSignal() === 'uploading');
  readonly isGridView = computed(() => this.viewModeSignal() === 'grid');
  readonly isListView = computed(() => this.viewModeSignal() === 'list');

  readonly canViewAssets = computed(() => this.hasPermission('ASSET_VIEW'));
  readonly canUploadAssets = computed(() => this.hasPermission('ASSET_UPLOAD'));
  readonly canDownloadAssets = computed(
    () => this.hasPermission('ASSET_VIEW') || this.hasPermission('CREATIVE_DOWNLOAD'),
  );
  readonly canDeleteAssets = computed(() => this.hasPermission('ASSET_DELETE'));
  readonly canEditAssets = computed(() => this.hasPermission('ASSET_UPDATE'));

  setSelectedProjectId(projectId: string | null): void {
    this.selectedProjectIdSignal.set(projectId);
  }

  setViewMode(mode: AssetViewMode): void {
    this.viewModeSignal.set(mode);
  }

  selectAsset(asset: Asset | null): void {
    this.selectedAssetSignal.set(asset);
  }

  async loadProjectAssets(projectId: string): Promise<void> {
    this.selectedProjectIdSignal.set(projectId);
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    await this.runLoader(async () => {
      const page = await this.assetService.listProjectAssets(
        workspaceId,
        projectId,
        this.filtersSignal(),
        this.paginationSignal().page,
        this.paginationSignal().size,
        this.assetRequestContext(),
      );

      this.assetsSignal.set(page.items);
      this.paginationSignal.set(page.pagination);
      this.syncSelectedAsset();
    });
  }

  async loadAssetDetail(assetId: string): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    await this.runLoader(async () => {
      const asset = await this.assetService.getAsset(workspaceId, assetId, this.assetRequestContext());
      this.selectedAssetSignal.set(asset);
      this.selectedProjectIdSignal.set(asset.projectCampaignId);
      this.assetsSignal.update((assets) => upsertAsset(assets, asset));
    });
  }

  async applyFilters(filters: AssetFilter): Promise<void> {
    this.filtersSignal.set(filters);
    this.paginationSignal.update((pagination) => ({ ...pagination, page: 0 }));

    const projectId = this.selectedProjectIdSignal();
    if (projectId) {
      await this.loadProjectAssets(projectId);
    }
  }

  async goToPage(page: number): Promise<void> {
    this.paginationSignal.update((pagination) => ({ ...pagination, page }));
    const projectId = this.selectedProjectIdSignal();
    if (projectId) {
      await this.loadProjectAssets(projectId);
    }
  }

  async uploadAsset(payload: UploadAssetPayload): Promise<AssetActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    const fieldErrors = validateUploadPayload(payload, workspaceId);

    if (Object.keys(fieldErrors).length > 0) {
      return { ok: false, fieldErrors };
    }

    if (!workspaceId) {
      return {
        ok: false,
        fieldErrors: { workspaceId: 'Select a workspace before uploading assets.' },
      };
    }

    this.cancelUpload();
    this.uploadStateSignal.set('uploading');
    this.uploadProgressSignal.set(0);

    return new Promise<AssetActionResult>((resolve) => {
      this.activeUploadSubscription = this.assetService
        .uploadProjectAsset(workspaceId, payload)
        .subscribe({
          next: (event) => {
            if (!event) {
              return;
            }

            if (event.kind === 'progress') {
              this.uploadProgressSignal.set(event.progress);
              return;
            }

            this.uploadStateSignal.set('completed');
            this.uploadProgressSignal.set(100);
            this.assetsSignal.update((assets) => upsertAsset(assets, event.asset));
            this.notifications.success('Asset uploaded', `${event.asset.displayName} is ready for review.`);
            resolve({ ok: true, fieldErrors: {} });
          },
          error: (error) => {
            const normalized = normalizeHttpError(error);
            this.uploadStateSignal.set('failed');
            this.uploadProgressSignal.set(null);
            this.notifications.error('Upload failed', normalized.message);
            resolve({ ok: false, message: normalized.message, fieldErrors: {} });
          },
          complete: () => {
            this.activeUploadSubscription = null;
            if (this.uploadStateSignal() === 'uploading') {
              this.uploadStateSignal.set('idle');
              this.uploadProgressSignal.set(null);
            }
          },
        });
    });
  }

  cancelUpload(): void {
    this.activeUploadSubscription?.unsubscribe();
    this.activeUploadSubscription = null;
    this.uploadStateSignal.set('idle');
    this.uploadProgressSignal.set(null);
  }

  async updateAsset(assetId: string, payload: UpdateAssetPayload): Promise<AssetActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return { ok: false, fieldErrors: {} };
    }

    try {
      const asset = await this.assetService.updateAsset(workspaceId, assetId, payload);
      this.assetsSignal.update((assets) => upsertAsset(assets, asset));
      if (this.selectedAssetSignal()?.id === asset.id) {
        this.selectedAssetSignal.set(asset);
      }
      this.notifications.success('Asset updated', 'Asset metadata was saved.');
      return { ok: true, fieldErrors: {} };
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.notifications.error('Update failed', normalized.message);
      return { ok: false, message: normalized.message, fieldErrors: {} };
    }
  }

  async deleteAsset(assetId: string): Promise<AssetActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return { ok: false, fieldErrors: {} };
    }

    try {
      await this.assetService.deleteAsset(workspaceId, assetId);
      this.assetsSignal.update((assets) => assets.filter((asset) => asset.id !== assetId));
      if (this.selectedAssetSignal()?.id === assetId) {
        this.selectedAssetSignal.set(null);
      }
      this.notifications.success('Asset deleted', 'The asset was removed from the project library.');
      return { ok: true, fieldErrors: {} };
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.notifications.error('Delete failed', normalized.message);
      return { ok: false, message: normalized.message, fieldErrors: {} };
    }
  }

  async getPreviewUrl(assetId: string) {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return null;
    }

    try {
      return await this.assetService.getPreviewUrl(workspaceId, assetId);
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.notifications.error('Preview unavailable', normalized.message);
      return null;
    }
  }

  async getDownloadUrl(assetId: string) {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return null;
    }

    try {
      return await this.assetService.getDownloadUrl(workspaceId, assetId);
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.notifications.error('Download unavailable', normalized.message);
      return null;
    }
  }

  private syncSelectedAsset(): void {
    const selected = this.selectedAssetSignal();
    if (!selected) {
      return;
    }

    const refreshed = this.assetsSignal().find((asset) => asset.id === selected.id);
    this.selectedAssetSignal.set(refreshed ?? null);
  }

  private async runLoader(task: () => Promise<void>): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      await task();
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.errorSignal.set(normalized.message);
      this.notifications.error('Asset library', normalized.message);
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private resolveWorkspaceId(): string | null {
    return this.auth.activeWorkspaceId();
  }

  private hasPermission(permission: Permission): boolean {
    return this.permissions.has(permission);
  }

  private assetRequestContext(): HttpContext {
    return new HttpContext().set(SKIP_ERROR_TOAST, true);
  }
}

function upsertAsset(assets: readonly Asset[], asset: Asset): readonly Asset[] {
  const index = assets.findIndex((item) => item.id === asset.id);
  if (index === -1) {
    return [asset, ...assets];
  }

  return assets.map((item, itemIndex) => (itemIndex === index ? asset : item));
}
