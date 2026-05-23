package com.lebhas.ai.job;

import com.lebhas.ai.cache.AiRedisAccessSupport;
import com.lebhas.ai.cache.AiRedisKeyConstants;
import com.lebhas.ai.cache.AiRedisOperationContext;
import com.lebhas.ai.cache.AiRedisTtlStrategy;

import java.util.Optional;
import java.util.UUID;

public class AiJobStateRedisService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiJobStateRedisService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<AiJobStateCacheEntry> get(UUID workspaceId, UUID creativeRequestId, UUID jobId, String provider) {
        return redisAccessSupport.read(
                AiRedisKeyConstants.aiJob(jobId),
                AiJobStateCacheEntry.class,
                "ai-job-state-read",
                AiRedisOperationContext.job(workspaceId, creativeRequestId, jobId, provider));
    }

    public boolean store(AiJobStateCacheEntry entry) {
        String provider = entry.providerType() == null ? null : entry.providerType().name();
        return redisAccessSupport.write(
                AiRedisKeyConstants.aiJob(entry.jobId()),
                entry,
                ttlStrategy.aiJobStateTtl(),
                "ai-job-state-write",
                AiRedisOperationContext.job(entry.workspaceId(), entry.creativeRequestId(), entry.jobId(), provider));
    }

    public boolean clear(UUID workspaceId, UUID creativeRequestId, UUID jobId, String provider) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.aiJob(jobId),
                "ai-job-state-delete",
                AiRedisOperationContext.job(workspaceId, creativeRequestId, jobId, provider));
    }
}
