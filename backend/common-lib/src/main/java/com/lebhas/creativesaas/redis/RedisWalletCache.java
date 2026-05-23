package com.lebhas.creativesaas.redis;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

@Service
public class RedisWalletCache {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final RedisCacheService redisCacheService;
    private final RedisKeyBuilder redisKeyBuilder;

    public RedisWalletCache(RedisCacheService redisCacheService, RedisKeyBuilder redisKeyBuilder) {
        this.redisCacheService = redisCacheService;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public WalletSnapshot getOrLoad(UUID workspaceId, Supplier<WalletSnapshot> loader) {
        return redisCacheService.getOrLoad(
                redisKeyBuilder.wallet(workspaceId),
                DEFAULT_TTL,
                WalletSnapshot.class,
                loader);
    }

    public Optional<WalletSnapshot> get(UUID workspaceId) {
        return redisCacheService.get(redisKeyBuilder.wallet(workspaceId), WalletSnapshot.class);
    }

    public void store(UUID workspaceId, WalletSnapshot snapshot) {
        redisCacheService.set(redisKeyBuilder.wallet(workspaceId), snapshot, DEFAULT_TTL);
    }

    public void invalidate(UUID workspaceId) {
        redisCacheService.delete(redisKeyBuilder.wallet(workspaceId));
    }

    public record WalletSnapshot(BigDecimal balance, BigDecimal reservedBalance, Instant cachedAt) {
    }
}
