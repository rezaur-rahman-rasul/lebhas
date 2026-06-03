package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetListCriteria;
import com.lebhas.creativesaas.asset.application.dto.AssetUrlView;
import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.application.dto.UpdateAssetCommand;
import com.lebhas.creativesaas.asset.application.dto.UploadAssetCommand;
import com.lebhas.creativesaas.asset.cache.AssetHotRedisCacheService;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class AssetService {

    private final AssetUploadService assetUploadService;
    private final AssetQueryService assetQueryService;
    private final AssetValidationService assetValidationService;
    private final AssetRepository assetRepository;
    private final AssetMetadataSerializer assetMetadataSerializer;
    private final AssetMapper assetMapper;
    private final AssetCacheService assetCacheService;
    private final AssetHotRedisCacheService assetHotRedisCacheService;
    private final SignedUrlService signedUrlService;
    private final PreviewStateService previewStateService;
    private final AssetActivityLogger assetActivityLogger;
    private final AssetEventPublisher assetEventPublisher;
    private final StorageService storageService;
    private final RedisLockService redisLockService;
    private final RedisKeyBuilder redisKeyBuilder;
    private final AssetStorageUsageService assetStorageUsageService;

    public AssetService(
            AssetUploadService assetUploadService,
            AssetQueryService assetQueryService,
            AssetValidationService assetValidationService,
            AssetRepository assetRepository,
            AssetMetadataSerializer assetMetadataSerializer,
            AssetMapper assetMapper,
            AssetCacheService assetCacheService,
            AssetHotRedisCacheService assetHotRedisCacheService,
            SignedUrlService signedUrlService,
            PreviewStateService previewStateService,
            AssetActivityLogger assetActivityLogger,
            AssetEventPublisher assetEventPublisher,
            StorageService storageService,
            RedisLockService redisLockService,
            RedisKeyBuilder redisKeyBuilder,
            AssetStorageUsageService assetStorageUsageService
    ) {
        this.assetUploadService = assetUploadService;
        this.assetQueryService = assetQueryService;
        this.assetValidationService = assetValidationService;
        this.assetRepository = assetRepository;
        this.assetMetadataSerializer = assetMetadataSerializer;
        this.assetMapper = assetMapper;
        this.assetCacheService = assetCacheService;
        this.assetHotRedisCacheService = assetHotRedisCacheService;
        this.signedUrlService = signedUrlService;
        this.previewStateService = previewStateService;
        this.assetActivityLogger = assetActivityLogger;
        this.assetEventPublisher = assetEventPublisher;
        this.storageService = storageService;
        this.redisLockService = redisLockService;
        this.redisKeyBuilder = redisKeyBuilder;
        this.assetStorageUsageService = assetStorageUsageService;
    }

    @Transactional
    public AssetView uploadAsset(UploadAssetCommand command) {
        return assetUploadService.uploadAsset(command);
    }

    @Transactional(readOnly = true)
    public PagedResult<AssetView> listAssets(AssetListCriteria criteria) {
        return assetQueryService.listAssets(criteria);
    }

    @Transactional(readOnly = true)
    public AssetView getAsset(UUID workspaceId, UUID assetId) {
        return assetQueryService.getAsset(workspaceId, assetId);
    }

    @Transactional
    public AssetView updateAsset(UpdateAssetCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = assetValidationService.requireUpdateAccess(command.workspaceId());
        RedisLockService.RedisLockToken lockToken = acquireAssetLock(command.assetId());
        try {
            AssetEntity asset = assetValidationService.requireAsset(command.workspaceId(), command.assetId());
            assetValidationService.validateOwnership(asset, access, Permission.ASSET_UPDATE);
            asset.updateDetails(
                    command.displayName(),
                    command.description(),
                    command.assetCategory(),
                    assetValidationService.normalizeTags(command.tags()),
                    assetMetadataSerializer.serialize(command.metadata()));
            AssetEntity saved = assetRepository.save(asset);
            assetCacheService.invalidate(saved.getWorkspaceId(), saved.getProjectId(), saved.getId(), access.currentUser().userId());
            AssetView view = assetMapper.toAssetView(saved);
            assetCacheService.cacheAsset(view);
            assetActivityLogger.logAssetUpdated(command.workspaceId(), saved.getId(), access.currentUser().userId());
            return view;
        } finally {
            redisLockService.release(lockToken);
        }
    }

    @Transactional
    public void deleteAsset(UUID workspaceId, UUID assetId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = assetValidationService.requireDeleteAccess(workspaceId);
        RedisLockService.RedisLockToken lockToken = acquireAssetLock(assetId);
        try {
            AssetEntity asset = assetValidationService.requireAsset(workspaceId, assetId);
            assetValidationService.validateOwnership(asset, access, Permission.ASSET_DELETE);
            long activeReferences = asset.getStorageFileId() == null ? 0L : assetRepository.countByStorageFileIdAndDeletedFalse(asset.getStorageFileId());
            boolean storageReleased = asset.getStorageFileId() != null && activeReferences <= 1L;
            signedUrlService.invalidate(asset);
            assetHotRedisCacheService.invalidate(workspaceId, assetId);
            previewStateService.invalidate(asset.getId());
            asset.markDeletedAsset();
            assetRepository.save(asset);
            assetStorageUsageService.recordSoftDelete(asset, storageReleased);
            assetCacheService.invalidate(workspaceId, asset.getProjectId(), asset.getId(), access.currentUser().userId());
            assetActivityLogger.logAssetDeleted(workspaceId, assetId, access.currentUser().userId());
            assetEventPublisher.publishDeleted(asset, storageReleased);
            assetEventPublisher.publishCleanup(asset, storageReleased);
        } finally {
            redisLockService.release(lockToken);
        }
    }

    @Transactional(readOnly = true)
    public AssetUrlView generatePreviewUrl(UUID workspaceId, UUID assetId) {
        return assetQueryService.generatePreviewUrl(workspaceId, assetId);
    }

    @Transactional(readOnly = true)
    public AssetUrlView generateDownloadUrl(UUID workspaceId, UUID assetId) {
        return assetQueryService.generateDownloadUrl(workspaceId, assetId);
    }

    @Transactional(readOnly = true)
    public AssetEntity requireAssetForSignedAccess(UUID assetId) {
        return assetQueryService.requireAssetForSignedAccess(assetId);
    }

    @Transactional(readOnly = true)
    public AssetEntity requireAsset(UUID workspaceId, UUID assetId) {
        return assetQueryService.requireAsset(workspaceId, assetId);
    }

    private RedisLockService.RedisLockToken acquireAssetLock(UUID assetId) {
        return redisLockService.acquire(redisKeyBuilder.lockAsset(assetId), Duration.ofSeconds(15))
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Asset mutation is already in progress"));
    }
}
