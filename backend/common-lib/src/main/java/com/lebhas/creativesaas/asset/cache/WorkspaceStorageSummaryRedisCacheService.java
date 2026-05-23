package com.lebhas.creativesaas.asset.cache;

import com.lebhas.creativesaas.asset.cache.dto.WorkspaceStorageSummaryCacheEntry;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class WorkspaceStorageSummaryRedisCacheService {

    private final AssetRedisAccessSupport redisAccessSupport;
    private final AssetCacheTtlStrategy ttlStrategy;

    public WorkspaceStorageSummaryRedisCacheService(
            AssetRedisAccessSupport redisAccessSupport,
            AssetCacheTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<WorkspaceStorageSummaryCacheEntry> get(UUID workspaceId) {
        return redisAccessSupport.read(
                AssetCacheKeys.workspaceStorage(workspaceId),
                WorkspaceStorageSummaryCacheEntry.class,
                workspaceId,
                null);
    }

    public void store(WorkspaceStorageSummaryCacheEntry entry) {
        redisAccessSupport.write(
                AssetCacheKeys.workspaceStorage(entry.workspaceId()),
                entry,
                ttlStrategy.workspaceStorageTtl(),
                entry.workspaceId(),
                null);
    }

    public void invalidate(UUID workspaceId) {
        redisAccessSupport.delete(AssetCacheKeys.workspaceStorage(workspaceId), workspaceId, null);
    }
}
