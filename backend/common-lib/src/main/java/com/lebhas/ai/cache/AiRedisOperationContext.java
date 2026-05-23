package com.lebhas.ai.cache;

import java.util.UUID;

public record AiRedisOperationContext(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID jobId,
        String provider
) {
    public static AiRedisOperationContext workspace(UUID workspaceId) {
        return new AiRedisOperationContext(workspaceId, null, null, null);
    }

    public static AiRedisOperationContext request(UUID workspaceId, UUID creativeRequestId) {
        return new AiRedisOperationContext(workspaceId, creativeRequestId, null, null);
    }

    public static AiRedisOperationContext job(UUID workspaceId, UUID creativeRequestId, UUID jobId, String provider) {
        return new AiRedisOperationContext(workspaceId, creativeRequestId, jobId, provider);
    }
}
