package com.lebhas.ai.cache;

import java.util.Optional;
import java.util.UUID;

public class AiGenerationProgressRedisService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiGenerationProgressRedisService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<AiGenerationProgressCacheEntry> get(UUID workspaceId, UUID creativeRequestId, UUID jobId) {
        return redisAccessSupport.read(
                AiRedisKeyConstants.aiProgress(creativeRequestId),
                AiGenerationProgressCacheEntry.class,
                "ai-progress-read",
                AiRedisOperationContext.job(workspaceId, creativeRequestId, jobId, null));
    }

    public boolean store(AiGenerationProgressCacheEntry entry) {
        return redisAccessSupport.write(
                AiRedisKeyConstants.aiProgress(entry.creativeRequestId()),
                entry,
                ttlStrategy.generationProgressTtl(),
                "ai-progress-write",
                AiRedisOperationContext.job(entry.workspaceId(), entry.creativeRequestId(), entry.jobId(), null));
    }

    public boolean clear(UUID workspaceId, UUID creativeRequestId, UUID jobId) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.aiProgress(creativeRequestId),
                "ai-progress-delete",
                AiRedisOperationContext.job(workspaceId, creativeRequestId, jobId, null));
    }
}
