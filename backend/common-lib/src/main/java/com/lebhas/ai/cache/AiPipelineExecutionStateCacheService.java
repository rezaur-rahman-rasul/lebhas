package com.lebhas.ai.cache;

import java.util.Optional;
import java.util.UUID;

public class AiPipelineExecutionStateCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiPipelineExecutionStateCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<PipelineExecutionStateCacheEntry> get(UUID workspaceId, UUID creativeRequestId) {
        return redisAccessSupport.read(
                AiRedisKeyConstants.generationState(creativeRequestId),
                PipelineExecutionStateCacheEntry.class,
                "pipeline-execution-state-read",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }

    public boolean store(PipelineExecutionStateCacheEntry entry) {
        return redisAccessSupport.write(
                AiRedisKeyConstants.generationState(entry.creativeRequestId()),
                entry,
                ttlStrategy.pipelineExecutionStateTtl(),
                "pipeline-execution-state-write",
                AiRedisOperationContext.request(entry.workspaceId(), entry.creativeRequestId()));
    }

    public boolean clear(UUID workspaceId, UUID creativeRequestId) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.generationState(creativeRequestId),
                "pipeline-execution-state-delete",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }
}
