package com.lebhas.creativesaas.redis;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RedisSignedUrlCache {

    private final RedisCacheService redisCacheService;
    private final RedisKeyBuilder redisKeyBuilder;

    public RedisSignedUrlCache(RedisCacheService redisCacheService, RedisKeyBuilder redisKeyBuilder) {
        this.redisCacheService = redisCacheService;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public void store(UUID storageFileId, SignedUrlSnapshot snapshot, Duration ttl) {
        redisCacheService.set(redisKeyBuilder.signedUrl(storageFileId), snapshot, ttl);
    }

    public Optional<SignedUrlSnapshot> get(UUID storageFileId) {
        return redisCacheService.get(redisKeyBuilder.signedUrl(storageFileId), SignedUrlSnapshot.class);
    }

    public void invalidate(UUID storageFileId) {
        redisCacheService.delete(redisKeyBuilder.signedUrl(storageFileId));
    }

    public record SignedUrlSnapshot(
            String url,
            Instant expiresAt,
            String type,
            String cdnUrl,
            Instant generatedAt
    ) {
    }
}
