package com.lebhas.ai.cache;

import com.lebhas.ai.application.dto.WorkspaceAiUsageView;

import java.util.Optional;
import java.util.UUID;

public class WorkspaceAiUsageCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public WorkspaceAiUsageCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<WorkspaceAiUsageView> get(UUID workspaceId) {
        return redisAccessSupport.read(
                AiAnalyticsRedisKeys.workspaceUsage(workspaceId),
                WorkspaceAiUsageView.class,
                "ai-workspace-usage-cache-read",
                new AiRedisOperationContext(workspaceId, null, null, null));
    }

    public boolean store(WorkspaceAiUsageView view) {
        return redisAccessSupport.write(
                AiAnalyticsRedisKeys.workspaceUsage(view.workspaceId()),
                view,
                ttlStrategy.workspaceUsageTtl(),
                "ai-workspace-usage-cache-write",
                new AiRedisOperationContext(view.workspaceId(), null, null, null));
    }

    public boolean invalidate(UUID workspaceId) {
        return redisAccessSupport.delete(
                AiAnalyticsRedisKeys.workspaceUsage(workspaceId),
                "ai-workspace-usage-cache-delete",
                new AiRedisOperationContext(workspaceId, null, null, null));
    }
}
