package com.lebhas.creativesaas.storage.application;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.storage.cache.StorageUsageRedisCacheService;
import com.lebhas.creativesaas.storage.cache.dto.StorageUsageCacheEntry;
import com.lebhas.creativesaas.storage.domain.StorageUsageEntity;
import com.lebhas.creativesaas.storage.infrastructure.persistence.StorageUsageRepository;
import com.lebhas.creativesaas.storage.producer.StoragePlanEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class StorageUsageService {

    private static final Logger log = LoggerFactory.getLogger(StorageUsageService.class);

    private final StorageUsageRepository storageUsageRepository;
    private final StorageUsageRedisCacheService storageUsageRedisCacheService;
    private final StorageUsageMapper storageUsageMapper;
    private final StoragePlanEventProducer storagePlanEventProducer;

    public StorageUsageService(
            StorageUsageRepository storageUsageRepository,
            StorageUsageRedisCacheService storageUsageRedisCacheService,
            StorageUsageMapper storageUsageMapper,
            StoragePlanEventProducer storagePlanEventProducer
    ) {
        this.storageUsageRepository = storageUsageRepository;
        this.storageUsageRedisCacheService = storageUsageRedisCacheService;
        this.storageUsageMapper = storageUsageMapper;
        this.storagePlanEventProducer = storagePlanEventProducer;
    }

    @Transactional(readOnly = true)
    public StorageUsageCacheEntry getUsage(UUID workspaceId) {
        return storageUsageRedisCacheService.get(workspaceId)
                .orElseGet(() -> {
                    StorageUsageEntity entity = storageUsageRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceId)
                            .orElseGet(() -> StorageUsageEntity.create(workspaceId, 0L, 0L, 0L, 0L, 0L, null));
                    StorageUsageCacheEntry entry = storageUsageMapper.toCacheEntry(entity, Instant.now());
                    storageUsageRedisCacheService.store(entry);
                    return entry;
                });
    }

    @Transactional(readOnly = true)
    public long getTotalUsedBytes(UUID workspaceId) {
        return getUsage(workspaceId).totalUsedBytes();
    }

    @Transactional
    public StorageUsageEntity getAuthoritativeSnapshot(UUID workspaceId) {
        StorageUsageEntity usage = findOrCreateManagedEntity(workspaceId);
        synchronizeCache(usage);
        return usage;
    }

    @Transactional
    public StorageUsageEntity getOrCreateSnapshot(UUID workspaceId) {
        return getAuthoritativeSnapshot(workspaceId);
    }

    @Transactional
    public StorageUsageEntity recordUpload(UUID workspaceId, AssetType assetType, long addedBytes) {
        return recordUpload(workspaceId, null, assetType, addedBytes, null, "STORAGE_USAGE_UPDATED");
    }

    @Transactional
    public StorageUsageEntity recordUpload(
            UUID workspaceId,
            UUID assetId,
            AssetType assetType,
            long addedBytes,
            Long storageLimitBytes,
            String reason
    ) {
        StorageUsageEntity usage = findOrCreateManagedEntity(workspaceId);
        long normalizedBytes = Math.max(addedBytes, 0L);
        long rawBytes = usage.getRawAssetBytes() + (isGeneratedAsset(assetType) ? 0L : normalizedBytes);
        long generatedBytes = usage.getGeneratedAssetBytes() + (isGeneratedAsset(assetType) ? normalizedBytes : 0L);
        usage.updateTotals(
                usage.getTotalUsedBytes() + normalizedBytes,
                rawBytes,
                generatedBytes,
                usage.getVariantBytes(),
                usage.getDeletedBytes(),
                Instant.now());
        return saveAndSynchronize(usage, assetId, storageLimitBytes, reason);
    }

    @Transactional
    public StorageUsageEntity recordVariantStored(UUID workspaceId, long addedBytes) {
        StorageUsageEntity usage = findOrCreateManagedEntity(workspaceId);
        long normalizedBytes = Math.max(addedBytes, 0L);
        usage.updateTotals(
                usage.getTotalUsedBytes() + normalizedBytes,
                usage.getRawAssetBytes(),
                usage.getGeneratedAssetBytes(),
                usage.getVariantBytes() + normalizedBytes,
                usage.getDeletedBytes(),
                Instant.now());
        return saveAndSynchronize(usage, null, null, "VARIANT_STORED");
    }

    @Transactional
    public StorageUsageEntity recordSoftDelete(AssetEntity asset, boolean storageReleased) {
        StorageUsageEntity usage = findOrCreateManagedEntity(asset.getWorkspaceId());
        long assetBytes = Math.max(asset.getFileSize(), 0L);
        long totalUsedBytes = usage.getTotalUsedBytes();
        long deletedBytes = usage.getDeletedBytes();
        long rawAssetBytes = usage.getRawAssetBytes();
        long generatedAssetBytes = usage.getGeneratedAssetBytes();
        if (storageReleased) {
            totalUsedBytes = Math.max(0L, totalUsedBytes - assetBytes);
            deletedBytes = Math.max(0L, deletedBytes - assetBytes);
            if (isGeneratedAsset(asset.getAssetType())) {
                generatedAssetBytes = Math.max(0L, generatedAssetBytes - assetBytes);
            } else {
                rawAssetBytes = Math.max(0L, rawAssetBytes - assetBytes);
            }
        } else {
            deletedBytes = deletedBytes + assetBytes;
        }
        usage.updateTotals(
                totalUsedBytes,
                rawAssetBytes,
                generatedAssetBytes,
                usage.getVariantBytes(),
                deletedBytes,
                Instant.now());
        return saveAndSynchronize(usage, asset.getId(), null, storageReleased ? "ASSET_STORAGE_RELEASED" : "ASSET_SOFT_DELETED");
    }

    @Transactional
    public StorageUsageEntity replaceSnapshot(
            UUID workspaceId,
            long totalUsedBytes,
            long rawAssetBytes,
            long generatedAssetBytes,
            long variantBytes,
            long deletedBytes,
            Instant lastCalculatedAt
    ) {
        StorageUsageEntity usage = findOrCreateManagedEntity(workspaceId);
        usage.updateTotals(
                totalUsedBytes,
                rawAssetBytes,
                generatedAssetBytes,
                variantBytes,
                deletedBytes,
                lastCalculatedAt == null ? Instant.now() : lastCalculatedAt);
        return saveAndSynchronize(usage, null, null, "STORAGE_USAGE_RECALCULATED");
    }

    @Transactional
    public void invalidateCache(UUID workspaceId) {
        storageUsageRedisCacheService.invalidate(workspaceId);
    }

    private StorageUsageEntity findOrCreateManagedEntity(UUID workspaceId) {
        return storageUsageRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceId)
                .orElseGet(() -> storageUsageRepository.save(StorageUsageEntity.create(
                        workspaceId,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        Instant.now())));
    }

    private StorageUsageEntity saveAndSynchronize(
            StorageUsageEntity usage,
            UUID assetId,
            Long storageLimitBytes,
            String reason
    ) {
        StorageUsageEntity saved = storageUsageRepository.save(usage);
        synchronizeCache(saved);
        try {
            storagePlanEventProducer.publishStorageUsageUpdated(
                    storageUsageMapper.toUpdatedEvent(saved, assetId, storageLimitBytes, reason, Instant.now()));
        } catch (RuntimeException exception) {
            String publishFailureReason = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage().replaceAll("\\s+", " ").trim();
            log.warn("storage_usage_event type=publish_failed workspaceId={} storageUsageId={} reason={}",
                    saved.getWorkspaceId(),
                    saved.getId(),
                    publishFailureReason);
        }
        return saved;
    }

    private void synchronizeCache(StorageUsageEntity usage) {
        storageUsageRedisCacheService.invalidate(usage.getWorkspaceId());
        storageUsageRedisCacheService.store(storageUsageMapper.toCacheEntry(usage, Instant.now()));
    }

    private boolean isGeneratedAsset(AssetType assetType) {
        return assetType == AssetType.GENERATED
                || assetType == AssetType.GENERATED_CREATIVE
                || assetType == AssetType.EXPORT_IMAGE
                || assetType == AssetType.EXPORT_VIDEO;
    }
}
