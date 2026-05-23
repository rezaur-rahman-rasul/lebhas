package com.lebhas.creativesaas.asset.cache;

import com.lebhas.creativesaas.asset.cache.dto.AssetHotCacheEntry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssetHotRedisCacheService {

    private static final long HOT_THRESHOLD = 5L;

    private final AssetRedisAccessSupport redisAccessSupport;
    private final AssetCacheTtlStrategy ttlStrategy;

    public AssetHotRedisCacheService(
            AssetRedisAccessSupport redisAccessSupport,
            AssetCacheTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public AssetHotCacheEntry recordDownload(UUID workspaceId, UUID assetId, String downloadType) {
        AssetHotCacheEntry current = get(workspaceId, assetId).orElse(null);
        Instant now = Instant.now();
        long nextCount = current == null ? 1L : current.downloadCount() + 1L;
        AssetHotCacheEntry updated = new AssetHotCacheEntry(
                assetId,
                workspaceId,
                nextCount,
                nextCount >= HOT_THRESHOLD,
                downloadType,
                current == null ? now : current.firstDownloadedAt(),
                now,
                now);
        redisAccessSupport.write(
                AssetCacheKeys.assetHot(assetId),
                updated,
                ttlStrategy.hotAssetTtl(),
                workspaceId,
                assetId);
        return updated;
    }

    public Optional<AssetHotCacheEntry> get(UUID workspaceId, UUID assetId) {
        return redisAccessSupport.read(
                AssetCacheKeys.assetHot(assetId),
                AssetHotCacheEntry.class,
                workspaceId,
                assetId);
    }

    public void invalidate(UUID workspaceId, UUID assetId) {
        redisAccessSupport.delete(AssetCacheKeys.assetHot(assetId), workspaceId, assetId);
    }
}
