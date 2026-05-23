package com.lebhas.creativesaas.generation.cache;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class GeneratedVersionCountCacheService {

    private final GenerationRedisKeys redisKeys;
    private final GenerationRedisAccessSupport redisAccessSupport;
    private final GenerationRedisTtlStrategy ttlStrategy;

    public GeneratedVersionCountCacheService(
            GenerationRedisKeys redisKeys,
            GenerationRedisAccessSupport redisAccessSupport,
            GenerationRedisTtlStrategy ttlStrategy
    ) {
        this.redisKeys = redisKeys;
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<GeneratedVersionCountCacheEntry> get(UUID workspaceId, UUID creativeRequestId) {
        return redisAccessSupport.read(
                redisKeys.generatedVersions(creativeRequestId),
                GeneratedVersionCountCacheEntry.class,
                "generated_version_count_get",
                GenerationRedisOperationContext.request(workspaceId, creativeRequestId));
    }

    public boolean store(UUID workspaceId, UUID creativeRequestId, int generatedVersionCount) {
        return store(new GeneratedVersionCountCacheEntry(
                workspaceId,
                creativeRequestId,
                Math.max(0, generatedVersionCount),
                Instant.now()));
    }

    public boolean store(GeneratedVersionCountCacheEntry entry) {
        if (entry == null || entry.creativeRequestId() == null) {
            return false;
        }
        return redisAccessSupport.write(
                redisKeys.generatedVersions(entry.creativeRequestId()),
                entry,
                ttlStrategy.generatedVersionCountTtl(),
                "generated_version_count_put",
                GenerationRedisOperationContext.request(entry.workspaceId(), entry.creativeRequestId()));
    }

    public boolean invalidate(UUID workspaceId, UUID creativeRequestId) {
        return redisAccessSupport.delete(
                redisKeys.generatedVersions(creativeRequestId),
                "generated_version_count_delete",
                GenerationRedisOperationContext.request(workspaceId, creativeRequestId));
    }

    public record GeneratedVersionCountCacheEntry(
            UUID workspaceId,
            UUID creativeRequestId,
            int generatedVersionCount,
            Instant cachedAt
    ) {
    }
}
