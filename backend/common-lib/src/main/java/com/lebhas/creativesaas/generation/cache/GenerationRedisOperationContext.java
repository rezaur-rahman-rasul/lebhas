package com.lebhas.creativesaas.generation.cache;

import java.util.UUID;

public record GenerationRedisOperationContext(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID jobId,
        String provider
) {

    public static GenerationRedisOperationContext job(UUID workspaceId, UUID creativeRequestId, UUID jobId) {
        return new GenerationRedisOperationContext(workspaceId, creativeRequestId, jobId, null);
    }

    public static GenerationRedisOperationContext request(UUID workspaceId, UUID creativeRequestId) {
        return new GenerationRedisOperationContext(workspaceId, creativeRequestId, null, null);
    }

    public static GenerationRedisOperationContext workspace(UUID workspaceId) {
        return new GenerationRedisOperationContext(workspaceId, null, null, null);
    }

    public static GenerationRedisOperationContext provider(String provider) {
        return new GenerationRedisOperationContext(null, null, null, provider);
    }
}
