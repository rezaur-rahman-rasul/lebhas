package com.lebhas.creativesaas.redis;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisGenerationDeduplicationLock {

    private final RedisLockService redisLockService;
    private final RedisKeyBuilder redisKeyBuilder;

    public RedisGenerationDeduplicationLock(RedisLockService redisLockService, RedisKeyBuilder redisKeyBuilder) {
        this.redisLockService = redisLockService;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public Optional<RedisLockService.RedisLockToken> acquire(String requestHash, Duration ttl) {
        return redisLockService.acquire(redisKeyBuilder.generationDeduplication(requestHash), ttl);
    }

    public boolean release(RedisLockService.RedisLockToken token) {
        return redisLockService.release(token);
    }
}
