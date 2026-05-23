package com.lebhas.creativesaas.asset.cache;

import com.lebhas.creativesaas.asset.cache.dto.UploadProgressCacheEntry;
import com.lebhas.creativesaas.asset.cache.dto.UploadStateCacheEntry;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class TemporaryUploadStateRedisCacheService {

    private final AssetRedisAccessSupport redisAccessSupport;
    private final AssetCacheTtlStrategy ttlStrategy;

    public TemporaryUploadStateRedisCacheService(
            AssetRedisAccessSupport redisAccessSupport,
            AssetCacheTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public void storeState(UploadStateCacheEntry entry) {
        redisAccessSupport.write(
                AssetCacheKeys.uploadState(entry.uploadId()),
                entry,
                ttlStrategy.temporaryUploadStateTtl(),
                entry.workspaceId(),
                entry.assetId());
    }

    public void storeProgress(UUID workspaceId, UUID assetId, UploadProgressCacheEntry entry) {
        redisAccessSupport.write(
                AssetCacheKeys.uploadProgress(entry.uploadId()),
                entry,
                ttlStrategy.temporaryUploadStateTtl(),
                workspaceId,
                assetId);
    }

    public Optional<UploadStateCacheEntry> getState(String uploadId, UUID workspaceId, UUID assetId) {
        return redisAccessSupport.read(
                AssetCacheKeys.uploadState(uploadId),
                UploadStateCacheEntry.class,
                workspaceId,
                assetId);
    }

    public Optional<UploadProgressCacheEntry> getProgress(String uploadId, UUID workspaceId, UUID assetId) {
        return redisAccessSupport.read(
                AssetCacheKeys.uploadProgress(uploadId),
                UploadProgressCacheEntry.class,
                workspaceId,
                assetId);
    }

    public void invalidate(String uploadId, UUID workspaceId, UUID assetId) {
        redisAccessSupport.delete(AssetCacheKeys.uploadState(uploadId), workspaceId, assetId);
        redisAccessSupport.delete(AssetCacheKeys.uploadProgress(uploadId), workspaceId, assetId);
    }
}
