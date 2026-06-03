package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.cache.AssetHotRedisCacheService;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class AssetHardDeleteService {

    private static final Logger log = LoggerFactory.getLogger(AssetHardDeleteService.class);

    private final AssetRepository assetRepository;
    private final JdbcTemplate jdbcTemplate;
    private final StorageService storageService;
    private final SignedUrlService signedUrlService;
    private final AssetHotRedisCacheService assetHotRedisCacheService;
    private final PreviewStateService previewStateService;
    private final AssetCacheService assetCacheService;
    private final AssetStorageUsageService assetStorageUsageService;
    private final AssetActivityLogger assetActivityLogger;
    private final AssetEventPublisher assetEventPublisher;

    public AssetHardDeleteService(
            AssetRepository assetRepository,
            JdbcTemplate jdbcTemplate,
            StorageService storageService,
            SignedUrlService signedUrlService,
            AssetHotRedisCacheService assetHotRedisCacheService,
            PreviewStateService previewStateService,
            AssetCacheService assetCacheService,
            AssetStorageUsageService assetStorageUsageService,
            AssetActivityLogger assetActivityLogger,
            AssetEventPublisher assetEventPublisher
    ) {
        this.assetRepository = assetRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
        this.signedUrlService = signedUrlService;
        this.assetHotRedisCacheService = assetHotRedisCacheService;
        this.previewStateService = previewStateService;
        this.assetCacheService = assetCacheService;
        this.assetStorageUsageService = assetStorageUsageService;
        this.assetActivityLogger = assetActivityLogger;
        this.assetEventPublisher = assetEventPublisher;
    }

    @Transactional
    public void deleteAssetAndStorage(AssetEntity asset, UUID actorUserId) {
        hardDelete(asset, actorUserId, true);
    }

    @Transactional
    public void deleteAssetRecordOnly(AssetEntity asset, UUID actorUserId) {
        hardDelete(asset, actorUserId, false);
    }

    private void hardDelete(AssetEntity asset, UUID actorUserId, boolean deleteStorage) {
        UUID workspaceId = asset.getWorkspaceId();
        UUID assetId = asset.getId();
        boolean storageReleased = !deleteStorage || !StringUtils.hasText(asset.getStorageKey());

        safeSideEffect("signed-url-cache", workspaceId, assetId, () -> signedUrlService.invalidate(asset));
        safeSideEffect("hot-cache", workspaceId, assetId, () -> assetHotRedisCacheService.invalidate(workspaceId, assetId));
        safeSideEffect("preview-state", workspaceId, assetId, () -> previewStateService.invalidate(assetId));
        safeSideEffect("asset-cache", workspaceId, assetId,
                () -> assetCacheService.invalidate(workspaceId, asset.getProjectId(), assetId, actorUserId));

        if (deleteStorage && StringUtils.hasText(asset.getStorageKey())) {
            storageReleased = deleteStorageObject(asset);
        }

        boolean finalStorageReleased = storageReleased;
        safeSideEffect("storage-usage", workspaceId, assetId,
                () -> assetStorageUsageService.recordSoftDelete(asset, finalStorageReleased));
        safeSideEffect("deleted-event", workspaceId, assetId,
                () -> assetEventPublisher.publishDeleted(asset, finalStorageReleased));
        safeSideEffect("activity-log", workspaceId, assetId,
                () -> assetActivityLogger.logAssetDeleted(workspaceId, assetId, actorUserId));

        clearDatabaseReferences(assetId);
        assetRepository.delete(asset);
        assetRepository.flush();

        log.info("Asset hard-deleted assetId={} workspaceId={} storageReleased={}",
                assetId,
                workspaceId,
                finalStorageReleased);
    }

    private boolean deleteStorageObject(AssetEntity asset) {
        try {
            storageService.delete(asset);
            return true;
        } catch (RuntimeException exception) {
            log.warn("Asset DB row will be hard-deleted but R2 cleanup failed assetId={} workspaceId={} storageKey={}",
                    asset.getId(),
                    asset.getWorkspaceId(),
                    asset.getStorageKey(),
                    exception);
            safeSideEffect("cleanup-event", asset.getWorkspaceId(), asset.getId(),
                    () -> assetEventPublisher.publishCleanup(asset, false));
            return false;
        }
    }

    private void clearDatabaseReferences(UUID assetId) {
        updateReference("creative_outputs.generated_asset_id",
                "UPDATE platform.creative_outputs SET generated_asset_id = NULL WHERE generated_asset_id = ?",
                assetId);
        updateReference("generated_versions.asset_id",
                "UPDATE platform.generated_versions SET asset_id = NULL WHERE asset_id = ?",
                assetId);
        updateReference("generated_versions.generated_asset_id",
                "UPDATE platform.generated_versions SET generated_asset_id = NULL WHERE generated_asset_id = ?",
                assetId);
        updateReference("generated_versions.preview_asset_id",
                "UPDATE platform.generated_versions SET preview_asset_id = NULL WHERE preview_asset_id = ?",
                assetId);
        updateReference("generated_versions.thumbnail_asset_id",
                "UPDATE platform.generated_versions SET thumbnail_asset_id = NULL WHERE thumbnail_asset_id = ?",
                assetId);
        updateReference("image_creative_generations.product_asset_id",
                "UPDATE platform.image_creative_generations SET product_asset_id = NULL WHERE product_asset_id = ?",
                assetId);
        updateReference("upload_sessions.asset_id",
                "UPDATE platform.upload_sessions SET asset_id = NULL WHERE asset_id = ?",
                assetId);
        updateReference("download_logs.asset_id",
                "UPDATE platform.download_logs SET asset_id = NULL WHERE asset_id = ?",
                assetId);
        updateReference("download_usage_logs.asset_id",
                "UPDATE platform.download_usage_logs SET asset_id = NULL WHERE asset_id = ?",
                assetId);
        updateReference("user_profiles.profile_image_asset_id",
                "UPDATE platform.user_profiles SET profile_image_asset_id = NULL WHERE profile_image_asset_id = ?",
                assetId);
    }

    private void updateReference(String reference, String sql, UUID assetId) {
        try {
            jdbcTemplate.update(sql, assetId);
        } catch (RuntimeException exception) {
            log.warn("Asset hard-delete reference cleanup failed reference={} assetId={}",
                    reference,
                    assetId,
                    exception);
        }
    }

    private void safeSideEffect(String operation, UUID workspaceId, UUID assetId, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.warn("Asset hard-delete side effect failed operation={} assetId={} workspaceId={}",
                    operation,
                    assetId,
                    workspaceId,
                    exception);
        }
    }
}
