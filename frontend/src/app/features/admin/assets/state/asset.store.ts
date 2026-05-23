import { HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom, Subscription } from 'rxjs';

import { normalizeHttpError } from '@app/core/api/http-error';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { ApiError } from '@app/shared/models/api-response.model';
import {
  Asset,
  AssetActionResult,
  AssetFilter,
  AssetFolder,
  AssetFolderSelection,
  AssetPagination,
  AssetStatus,
  AssetUrl,
  AssetViewMode,
  CreateAssetFolderPayload,
  DEFAULT_ASSET_FILTERS,
  DEFAULT_ASSET_PAGINATION,
  UpdateAssetFolderPayload,
  UpdateAssetPayload,
  UploadAssetPayload,
} from '../models/asset.models';
import { AssetService } from '../services/asset.service';

@Injectable({ providedIn: 'root' })
export class AssetStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly assetService = inject(AssetService);

  private readonly assetsSignal = signal<readonly Asset[]>([]);
  private readonly selectedAssetSignal = signal<Asset | null>(null);
  private readonly foldersSignal = signal<readonly AssetFolder[]>([]);
  private readonly filtersSignal = signal<AssetFilter>(DEFAULT_ASSET_FILTERS);
  private readonly selectedFolderSignal = signal<AssetFolderSelection>('all');
  private readonly paginationSignal = signal<AssetPagination>(DEFAULT_ASSET_PAGINATION);
  private readonly viewModeSignal = signal<AssetViewMode>('grid');
  private readonly uploadProgressSignal = signal<number | null>(null);
  private readonly assetLoadingSignal = signal(false);
  private readonly assetErrorSignal = signal<string | null>(null);

  private activeUploadSubscription: Subscription | null = null;

  readonly assets = this.assetsSignal.asReadonly();
  readonly selectedAsset = this.selectedAssetSignal.asReadonly();
  readonly folders = this.foldersSignal.asReadonly();
  readonly filters = this.filtersSignal.asReadonly();
  readonly selectedFolder = this.selectedFolderSignal.asReadonly();
  readonly pagination = this.paginationSignal.asReadonly();
  readonly viewMode = this.viewModeSignal.asReadonly();
  readonly uploadProgress = this.uploadProgressSignal.asReadonly();
  readonly assetLoading = this.assetLoadingSignal.asReadonly();
  readonly assetError = this.assetErrorSignal.asReadonly();

  readonly hasAssets = computed(() => this.filteredAssets().length > 0);
  readonly filteredAssets = computed(() => {
    const selectedFolder = this.selectedFolderSignal();
    const assets = this.assetsSignal();

    if (selectedFolder === 'all') {
      return assets;
    }

    if (selectedFolder === 'uncategorized') {
      return assets.filter((asset) => !asset.folderId);
    }

    return assets.filter((asset) => asset.folderId === selectedFolder);
  });
  readonly selectedFolderName = computed(() => {
    const selectedFolder = this.selectedFolderSignal();

    if (selectedFolder === 'all') {
      return 'All assets';
    }

    if (selectedFolder === 'uncategorized') {
      return 'Uncategorized';
    }

    return this.foldersSignal().find((folder) => folder.id === selectedFolder)?.name ?? 'Folder';
  });
  readonly isGridView = computed(() => this.viewModeSignal() === 'grid');
  readonly isListView = computed(() => this.viewModeSignal() === 'list');
  readonly canViewAssets = computed(() => this.hasPermission('ASSET_VIEW'));
  readonly canUploadAssets = computed(() => this.hasPermission('ASSET_UPLOAD'));
  readonly canEditAssets = computed(() => this.hasPermission('ASSET_UPDATE'));
  readonly canDeleteAssets = computed(() => this.hasPermission('ASSET_DELETE'));
  readonly canManageFolders = computed(() => this.hasPermission('ASSET_FOLDER_MANAGE'));
  readonly canDownloadAssets = computed(() => this.canViewAssets());

  async loadLibraryContext(): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    await this.runLoader(async () => {
      const [folders, page] = await Promise.all([
        firstValueFrom(this.assetService.listFolders(workspaceId, this.assetRequestContext())),
        firstValueFrom(
          this.assetService.listAssets(
            workspaceId,
            this.filtersSignal(),
            this.paginationSignal().page,
            this.paginationSignal().size,
            this.assetRequestContext(),
          ),
        ),
      ]);

      this.foldersSignal.set(folders);
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
      const [asset, folders] = await Promise.all([
        firstValueFrom(this.assetService.getAsset(workspaceId, assetId, this.assetRequestContext())),
        firstValueFrom(this.assetService.listFolders(workspaceId, this.assetRequestContext())),
      ]);

      this.selectedAssetSignal.set(asset);
      this.foldersSignal.set(folders);
      this.assetsSignal.update((assets) => upsertAsset(assets, asset));
    });
  }

  async reloadAssets(): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    await this.runLoader(async () => {
      const page = await firstValueFrom(
        this.assetService.listAssets(
          workspaceId,
          this.filtersSignal(),
          this.paginationSignal().page,
          this.paginationSignal().size,
          this.assetRequestContext(),
        ),
      );

      this.assetsSignal.set(page.items);
      this.paginationSignal.set(page.pagination);
      this.syncSelectedAsset();
    });
  }

  async applyFilters(filters: AssetFilter): Promise<void> {
    this.filtersSignal.set(filters);
    this.paginationSignal.update((pagination) => ({ ...pagination, page: 0 }));
    await this.reloadAssets();
  }

  async goToPage(page: number): Promise<void> {
    const pagination = this.paginationSignal();
    if (page < 0 || page >= pagination.totalPages || page === pagination.page) {
      return;
    }

    this.paginationSignal.update((current) => ({ ...current, page }));
    await this.reloadAssets();
  }

  setViewMode(viewMode: AssetViewMode): void {
    this.viewModeSignal.set(viewMode);
  }

  async selectFolder(folder: AssetFolderSelection): Promise<void> {
    this.selectedFolderSignal.set(folder);
    this.filtersSignal.update((filters) => ({
      ...filters,
      folderId: folder !== 'all' && folder !== 'uncategorized' ? folder : null,
    }));
    this.paginationSignal.update((pagination) => ({ ...pagination, page: 0 }));
    await this.reloadAssets();
  }

  selectAsset(asset: Asset | null): void {
    this.selectedAssetSignal.set(asset);
  }

  async uploadAsset(payload: UploadAssetPayload): Promise<AssetActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    this.cancelUpload(false);
    this.assetLoadingSignal.set(true);
    this.assetErrorSignal.set(null);
    this.uploadProgressSignal.set(0);

    return new Promise<AssetActionResult>((resolve) => {
      this.activeUploadSubscription = this.assetService.uploadAsset(workspaceId, payload).subscribe({
        next: (event) => {
          if (!event) {
            return;
          }

          if (event.kind === 'progress') {
            this.uploadProgressSignal.set(event.progress);
            return;
          }

          this.assetsSignal.update((assets) => [event.asset, ...assets.filter((item) => item.id !== event.asset.id)]);
          this.selectedAssetSignal.set(event.asset);
          this.paginationSignal.update((pagination) => ({
            ...pagination,
            totalItems: pagination.totalItems + 1,
          }));
          this.notifications.success('Asset uploaded', `${event.asset.originalFileName} is ready in the library.`);
          this.assetLoadingSignal.set(false);
          this.uploadProgressSignal.set(100);
          this.activeUploadSubscription?.unsubscribe();
          this.activeUploadSubscription = null;
          window.setTimeout(() => this.uploadProgressSignal.set(null), 400);
          resolve(this.successResult());
        },
        error: (error) => {
          this.activeUploadSubscription = null;
          this.uploadProgressSignal.set(null);
          this.assetLoadingSignal.set(false);
          resolve(this.failureResult(error));
        },
      });
    });
  }

  cancelUpload(notify = true): void {
    if (!this.activeUploadSubscription) {
      return;
    }

    this.activeUploadSubscription.unsubscribe();
    this.activeUploadSubscription = null;
    this.uploadProgressSignal.set(null);
    this.assetLoadingSignal.set(false);

    if (notify) {
      this.notifications.info('Upload cancelled', 'The current upload was cancelled.');
    }
  }

  async updateAsset(assetId: string, payload: UpdateAssetPayload, successMessage = 'Asset updated'): Promise<AssetActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.assetLoadingSignal.set(true);
      this.assetErrorSignal.set(null);
      const asset = await firstValueFrom(this.assetService.updateAsset(workspaceId, assetId, payload));
      this.assetsSignal.update((assets) => upsertAsset(assets, asset));
      this.selectedAssetSignal.set(asset);
      this.notifications.success(successMessage, `${asset.originalFileName} was updated.`);
      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.assetLoadingSignal.set(false);
    }
  }

  async deleteAsset(assetId: string): Promise<AssetActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.assetLoadingSignal.set(true);
      this.assetErrorSignal.set(null);
      await firstValueFrom(this.assetService.deleteAsset(workspaceId, assetId));
      this.assetsSignal.update((assets) => assets.filter((asset) => asset.id !== assetId));
      if (this.selectedAssetSignal()?.id === assetId) {
        this.selectedAssetSignal.set(null);
      }
      this.paginationSignal.update((pagination) => ({
        ...pagination,
        totalItems: Math.max(0, pagination.totalItems - 1),
      }));
      this.notifications.success('Asset deleted', 'The asset has been removed from this workspace.');
      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.assetLoadingSignal.set(false);
    }
  }

  async createFolder(payload: CreateAssetFolderPayload): Promise<AssetActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.assetLoadingSignal.set(true);
      this.assetErrorSignal.set(null);
      const folder = await firstValueFrom(this.assetService.createFolder(workspaceId, payload));
      this.foldersSignal.update((folders) => [...folders, folder]);
      this.notifications.success('Folder created', `${folder.name} is ready for asset organization.`);
      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.assetLoadingSignal.set(false);
    }
  }

  async renameFolder(folderId: string, payload: UpdateAssetFolderPayload): Promise<AssetActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.assetLoadingSignal.set(true);
      this.assetErrorSignal.set(null);
      const folder = await firstValueFrom(this.assetService.updateFolder(workspaceId, folderId, payload));
      this.foldersSignal.update((folders) =>
        folders.map((item) => (item.id === folder.id ? folder : item)),
      );
      this.notifications.success('Folder updated', `${folder.name} was updated.`);
      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.assetLoadingSignal.set(false);
    }
  }

  async deleteFolder(folderId: string): Promise<AssetActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.assetLoadingSignal.set(true);
      this.assetErrorSignal.set(null);
      await firstValueFrom(this.assetService.deleteFolder(workspaceId, folderId));
      this.foldersSignal.update((folders) => folders.filter((folder) => folder.id !== folderId));
      if (this.selectedFolderSignal() === folderId) {
        this.selectedFolderSignal.set('all');
        this.filtersSignal.update((filters) => ({ ...filters, folderId: null }));
      }
      this.notifications.success('Folder deleted', 'The folder has been removed.');
      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.assetLoadingSignal.set(false);
    }
  }

  async getPreviewUrl(assetId: string): Promise<AssetUrl | null> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      this.missingWorkspaceResult();
      return null;
    }

    try {
      return await firstValueFrom(this.assetService.getPreviewUrl(workspaceId, assetId));
    } catch (error) {
      this.failureResult(error);
      return null;
    }
  }

  async getDownloadUrl(assetId: string): Promise<AssetUrl | null> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      this.missingWorkspaceResult();
      return null;
    }

    try {
      return await firstValueFrom(this.assetService.getDownloadUrl(workspaceId, assetId));
    } catch (error) {
      this.failureResult(error);
      return null;
    }
  }

  private async runLoader(operation: () => Promise<void>): Promise<void> {
    try {
      this.assetLoadingSignal.set(true);
      this.assetErrorSignal.set(null);
      await operation();
    } catch (error) {
      this.assetErrorSignal.set(this.mapAssetLoadError(error));
    } finally {
      this.assetLoadingSignal.set(false);
    }
  }

  private resolveWorkspaceId(): string | null {
    const workspaceId = this.auth.activeWorkspaceId();

    if (workspaceId) {
      return workspaceId;
    }

    this.assetErrorSignal.set('Select a workspace before opening asset operations.');
    return null;
  }

  private hasPermission(permission: 'ASSET_VIEW' | 'ASSET_UPLOAD' | 'ASSET_UPDATE' | 'ASSET_DELETE' | 'ASSET_FOLDER_MANAGE'): boolean {
    return this.auth.permissions().includes(permission);
  }

  private syncSelectedAsset(): void {
    const selectedAssetId = this.selectedAssetSignal()?.id;
    if (!selectedAssetId) {
      return;
    }

    const refreshedAsset = this.assetsSignal().find((asset) => asset.id === selectedAssetId);
    if (refreshedAsset) {
      this.selectedAssetSignal.set(refreshedAsset);
    }
  }

  private successResult(): AssetActionResult {
    return { ok: true, fieldErrors: {} };
  }

  private missingWorkspaceResult(): AssetActionResult {
    const message = 'Select a workspace before opening asset operations.';
    this.assetErrorSignal.set(message);
    return { ok: false, message, fieldErrors: {} };
  }

  private failureResult(error: unknown): AssetActionResult {
    const normalized = normalizeHttpError(error);
    this.assetErrorSignal.set(normalized.message);
    return {
      ok: false,
      message: normalized.message,
      fieldErrors: this.mapFieldErrors(normalized.errors),
    };
  }

  private mapFieldErrors(errors: readonly ApiError[]): Readonly<Record<string, string>> {
    return errors.reduce<Record<string, string>>((result, error) => {
      if (error.field) {
        result[error.field] = error.message;
      }
      return result;
    }, {});
  }

  private mapAssetLoadError(error: unknown): string {
    const normalized = normalizeHttpError(error);

    if (normalized.status === 403) {
      return 'You do not have access to this workspace asset library.';
    }

    if (normalized.status === 404) {
      return 'This workspace asset library could not be found.';
    }

    if (normalized.status >= 500 || normalized.message === 'Unexpected server error') {
      return 'Asset data could not be loaded right now.';
    }

    return normalized.message;
  }

  private assetRequestContext(): HttpContext {
    return new HttpContext().set(SKIP_ERROR_TOAST, true);
  }
}

function upsertAsset(assets: readonly Asset[], asset: Asset): readonly Asset[] {
  const withoutCurrent = assets.filter((item) => item.id !== asset.id);
  return [asset, ...withoutCurrent];
}
