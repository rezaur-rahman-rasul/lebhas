package com.lebhas.creativesaas.generation.cache;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkspaceQuotaCacheService {

    private final GenerationRedisKeys redisKeys;
    private final GenerationRedisAccessSupport redisAccessSupport;
    private final GenerationRedisTtlStrategy ttlStrategy;

    public WorkspaceQuotaCacheService(
            GenerationRedisKeys redisKeys,
            GenerationRedisAccessSupport redisAccessSupport,
            GenerationRedisTtlStrategy ttlStrategy
    ) {
        this.redisKeys = redisKeys;
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<WorkspaceQuotaCacheEntry> get(UUID workspaceId) {
        return redisAccessSupport.read(
                redisKeys.workspaceQuota(workspaceId),
                WorkspaceQuotaCacheEntry.class,
                "workspace_quota_get",
                GenerationRedisOperationContext.workspace(workspaceId));
    }

    public boolean store(WorkspaceQuotaCacheEntry entry) {
        if (entry == null || entry.workspaceId() == null) {
            return false;
        }
        return redisAccessSupport.write(
                redisKeys.workspaceQuota(entry.workspaceId()),
                entry,
                ttlStrategy.workspaceQuotaTtl(),
                "workspace_quota_put",
                GenerationRedisOperationContext.workspace(entry.workspaceId()));
    }

    public boolean invalidate(UUID workspaceId) {
        return redisAccessSupport.delete(
                redisKeys.workspaceQuota(workspaceId),
                "workspace_quota_delete",
                GenerationRedisOperationContext.workspace(workspaceId));
    }

    public record WorkspaceQuotaCacheEntry(
            UUID workspaceId,
            Integer maxGeneratedVersionsPerRequest,
            Integer currentGeneratedVersions,
            BigDecimal monthlyCreditLimit,
            BigDecimal availableCredits,
            BigDecimal reservedCredits,
            Instant cachedAt
    ) {
        public WorkspaceQuotaCacheEntry {
            if (cachedAt == null) {
                cachedAt = Instant.now();
            }
        }
    }
}
