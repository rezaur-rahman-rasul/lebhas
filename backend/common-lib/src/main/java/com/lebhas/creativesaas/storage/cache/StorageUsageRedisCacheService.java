package com.lebhas.creativesaas.storage.cache;

import com.lebhas.creativesaas.storage.cache.dto.StorageUsageCacheEntry;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class StorageUsageRedisCacheService {

    private final StorageRedisAccessSupport redisAccessSupport;
    private final StorageUsageCacheTtlStrategy ttlStrategy;

    public StorageUsageRedisCacheService(
            StorageRedisAccessSupport redisAccessSupport,
            StorageUsageCacheTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<StorageUsageCacheEntry> get(UUID workspaceId) {
        return redisAccessSupport.read(
                StorageUsageRedisKeys.storageUsage(workspaceId),
                StorageUsageCacheEntry.class,
                workspaceId);
    }

    public void store(StorageUsageCacheEntry entry) {
        redisAccessSupport.write(
                StorageUsageRedisKeys.storageUsage(entry.workspaceId()),
                entry,
                ttlStrategy.storageUsageTtl(),
                entry.workspaceId());
    }

    public void invalidate(UUID workspaceId) {
        redisAccessSupport.delete(StorageUsageRedisKeys.storageUsage(workspaceId), workspaceId);
    }
}
