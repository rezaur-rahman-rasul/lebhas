import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { Permission } from '@app/features/auth/models/user.models';
import { Asset } from '../models/asset.models';
import { AssetApiService } from '../services/asset-api.service';
import { AssetStore } from './asset.store';

const sampleAsset: Asset = {
  id: 'asset-1',
  workspaceId: 'workspace-1',
  brandId: 'brand-1',
  productServiceId: 'product-1',
  projectCampaignId: 'project-1',
  storageFileId: 'file-1',
  uploadedBy: 'user-1',
  assetType: 'IMAGE',
  assetCategory: 'PRODUCT_IMAGE',
  originalFileName: 'product.jpg',
  displayName: 'Product flat lay',
  description: null,
  tags: [],
  uploadSessionId: null,
  previewStatus: 'READY',
  processingStatus: 'READY',
  status: 'READY',
  createdAt: '2026-05-17T00:00:00.000Z',
  updatedAt: '2026-05-17T00:00:00.000Z',
  storageFile: {
    id: 'file-1',
    workspaceId: 'workspace-1',
    provider: 's3',
    bucket: null,
    objectKey: 'product.jpg',
    cdnUrl: null,
    mimeType: 'image/jpeg',
    fileExtension: 'jpg',
    fileSize: 1024,
    hash: null,
    width: null,
    height: null,
    duration: null,
    storageClass: null,
    filePurpose: null,
    createdAt: '2026-05-17T00:00:00.000Z',
    updatedAt: '2026-05-17T00:00:00.000Z',
  },
};

describe('AssetStore', () => {
  let store: AssetStore;
  let assetService: {
    listProjectAssets: ReturnType<typeof vi.fn>;
    getPreviewUrl: ReturnType<typeof vi.fn>;
    getDownloadUrl: ReturnType<typeof vi.fn>;
    uploadProjectAsset: ReturnType<typeof vi.fn>;
  };
  let auth: {
    activeWorkspaceId: ReturnType<typeof vi.fn>;
    permissions: () => readonly Permission[];
  };
  let permissions: readonly Permission[];

  beforeEach(() => {
    assetService = {
      listProjectAssets: vi.fn(),
      getPreviewUrl: vi.fn(),
      getDownloadUrl: vi.fn(),
      uploadProjectAsset: vi.fn(),
    };

    permissions = ['ASSET_VIEW', 'ASSET_UPLOAD', 'ASSET_DELETE'];
    auth = {
      activeWorkspaceId: vi.fn(() => 'workspace-1'),
      permissions: () => permissions,
    };

    TestBed.configureTestingModule({
      providers: [
        AssetStore,
        { provide: AssetApiService, useValue: assetService },
        { provide: CurrentUserStore, useValue: auth },
        {
          provide: PermissionStore,
          useValue: {
            has: (permission: Permission) => auth.permissions().includes(permission),
          },
        },
        {
          provide: NotificationStateService,
          useValue: { success: vi.fn(), error: vi.fn() },
        },
      ],
    });

    store = TestBed.inject(AssetStore);
  });

  it('loads project-scoped assets', async () => {
    assetService.listProjectAssets.mockResolvedValue({
      items: [sampleAsset],
      pagination: {
        page: 0,
        size: 24,
        totalItems: 1,
        totalPages: 1,
        first: true,
        last: true,
      },
    });

    await store.loadProjectAssets('project-1');

    expect(assetService.listProjectAssets).toHaveBeenCalled();
    expect(store.assets().length).toBe(1);
  });

  it('computes admin upload permission', () => {
    expect(store.canUploadAssets()).toBe(true);
  });

  it('hides upload permission for crew without access', () => {
    permissions = ['ASSET_VIEW'];
    expect(store.canUploadAssets()).toBe(false);
  });

  it('surfaces API failures', async () => {
    assetService.listProjectAssets.mockRejectedValue({ error: { message: 'Request failed' } });

    await store.loadProjectAssets('project-1');

    expect(store.assetError()).toBeTruthy();
  });

  it('requests preview and download urls', async () => {
    assetService.getPreviewUrl.mockResolvedValue({
      url: 'https://preview',
      expiresAt: '2026-05-18T00:00:00.000Z',
    });
    assetService.getDownloadUrl.mockResolvedValue({
      url: 'https://download',
      expiresAt: '2026-05-18T00:00:00.000Z',
    });

    await store.getPreviewUrl('asset-1');
    await store.getDownloadUrl('asset-1');

    expect(assetService.getPreviewUrl).toHaveBeenCalledWith('workspace-1', 'asset-1');
    expect(assetService.getDownloadUrl).toHaveBeenCalledWith('workspace-1', 'asset-1');
  });

  it('uploads valid files through the API', async () => {
    assetService.uploadProjectAsset.mockReturnValue(
      of({ kind: 'completed', asset: sampleAsset } as const),
    );

    const file = new File([new ArrayBuffer(16)], 'product.jpg', { type: 'image/jpeg' });
    const result = await store.uploadAsset({
      projectId: 'project-1',
      file,
      assetCategory: 'PRODUCT_IMAGE',
      displayName: 'Product flat lay',
      description: '',
      tags: [],
    });

    expect(result.ok).toBe(true);
    expect(assetService.uploadProjectAsset).toHaveBeenCalled();
  });

  it('tracks upload progress state', async () => {
    const upload$ = new Subject<
      { kind: 'progress'; progress: number } | { kind: 'completed'; asset: Asset }
    >();
    assetService.uploadProjectAsset.mockReturnValue(upload$);

    const file = new File([new ArrayBuffer(16)], 'product.jpg', { type: 'image/jpeg' });
    const uploadPromise = store.uploadAsset({
      projectId: 'project-1',
      file,
      assetCategory: 'PRODUCT_IMAGE',
      displayName: 'Product flat lay',
      description: '',
      tags: [],
    });

    expect(store.isUploading()).toBe(true);

    upload$.next({ kind: 'progress', progress: 42 });
    expect(store.uploadProgress()).toBe(42);

    upload$.next({ kind: 'completed', asset: sampleAsset });
    upload$.complete();
    await uploadPromise;
  });
});
