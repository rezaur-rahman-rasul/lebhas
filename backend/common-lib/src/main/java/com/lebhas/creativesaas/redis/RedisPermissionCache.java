package com.lebhas.creativesaas.redis;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class RedisPermissionCache {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final RedisCacheService redisCacheService;
    private final RedisKeyBuilder redisKeyBuilder;

    public RedisPermissionCache(RedisCacheService redisCacheService, RedisKeyBuilder redisKeyBuilder) {
        this.redisCacheService = redisCacheService;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public PermissionSnapshot getOrLoad(UUID workspaceId, UUID userId, Supplier<PermissionSnapshot> loader) {
        return getOrLoad(workspaceId, userId, 1L, loader);
    }

    public PermissionSnapshot getOrLoad(
            UUID workspaceId,
            UUID userId,
            long expectedVersion,
            Supplier<PermissionSnapshot> loader
    ) {
        Optional<PermissionSnapshot> cached = get(workspaceId, userId);
        if (cached.isPresent() && cached.get().version() == expectedVersion) {
            return cached.get();
        }
        PermissionSnapshot loaded = loader.get();
        PermissionSnapshot versioned = new PermissionSnapshot(
                loaded.workspaceId(),
                loaded.userId(),
                loaded.permissions(),
                expectedVersion,
                Instant.now());
        store(workspaceId, userId, versioned);
        return versioned;
    }

    public Optional<PermissionSnapshot> get(UUID workspaceId, UUID userId) {
        return redisCacheService.get(redisKeyBuilder.permissions(workspaceId, userId), PermissionSnapshot.class);
    }

    public void invalidate(UUID workspaceId, UUID userId) {
        redisCacheService.delete(redisKeyBuilder.permissions(workspaceId, userId));
    }

    public void store(UUID workspaceId, UUID userId, PermissionSnapshot snapshot) {
        redisCacheService.set(redisKeyBuilder.permissions(workspaceId, userId), snapshot, DEFAULT_TTL);
    }

    public record PermissionSnapshot(
            UUID workspaceId,
            UUID userId,
            Set<String> permissions,
            long version,
            Instant cachedAt
    ) {
    }
}
