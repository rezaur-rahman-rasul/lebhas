package com.lebhas.creativesaas.redis;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class RedisWorkspaceContextCache {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final RedisCacheService redisCacheService;
    private final RedisKeyBuilder redisKeyBuilder;

    public RedisWorkspaceContextCache(RedisCacheService redisCacheService, RedisKeyBuilder redisKeyBuilder) {
        this.redisCacheService = redisCacheService;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public WorkspaceContextSnapshot getOrLoad(
            UUID workspaceId,
            UUID userId,
            Supplier<WorkspaceContextSnapshot> loader
    ) {
        return redisCacheService.getOrLoad(
                redisKeyBuilder.workspaceContext(workspaceId, userId),
                DEFAULT_TTL,
                WorkspaceContextSnapshot.class,
                loader);
    }

    public Optional<WorkspaceContextSnapshot> get(UUID workspaceId, UUID userId) {
        return redisCacheService.get(redisKeyBuilder.workspaceContext(workspaceId, userId), WorkspaceContextSnapshot.class);
    }

    public void invalidate(UUID workspaceId, UUID userId) {
        redisCacheService.delete(redisKeyBuilder.workspaceContext(workspaceId, userId));
    }

    public record WorkspaceContextSnapshot(
            UUID workspaceId,
            UUID userId,
            String role,
            Set<String> permissions,
            boolean canDownloadCreative,
            boolean canEditCreative,
            Instant cachedAt
    ) {
    }
}
