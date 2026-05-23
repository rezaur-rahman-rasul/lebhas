package com.lebhas.creativesaas.asset.cache;

import com.lebhas.creativesaas.asset.cache.dto.AssetSignedUrlCacheEntry;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AssetSignedUrlRedisCacheService {

    private static final String PREVIEW = "preview";
    private static final String DOWNLOAD = "download";

    private final AssetRedisAccessSupport redisAccessSupport;
    private final AssetCacheTtlStrategy ttlStrategy;

    public AssetSignedUrlRedisCacheService(
            AssetRedisAccessSupport redisAccessSupport,
            AssetCacheTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<AssetSignedUrlCacheEntry> get(UUID workspaceId, UUID assetId, String type) {
        return redisAccessSupport.read(
                AssetCacheKeys.signedUrl(assetId, type),
                AssetSignedUrlCacheEntry.class,
                workspaceId,
                assetId);
    }

    public void store(UUID workspaceId, UUID assetId, AssetSignedUrlCacheEntry entry) {
        redisAccessSupport.write(
                AssetCacheKeys.signedUrl(assetId, entry.type()),
                entry,
                ttlStrategy.signedUrlTtl(entry.expiresAt()),
                workspaceId,
                assetId);
    }

    public void invalidate(UUID workspaceId, UUID assetId) {
        invalidate(workspaceId, assetId, PREVIEW);
        invalidate(workspaceId, assetId, DOWNLOAD);
    }

    public void invalidate(UUID workspaceId, UUID assetId, String type) {
        redisAccessSupport.delete(AssetCacheKeys.signedUrl(assetId, type), workspaceId, assetId);
    }
}
