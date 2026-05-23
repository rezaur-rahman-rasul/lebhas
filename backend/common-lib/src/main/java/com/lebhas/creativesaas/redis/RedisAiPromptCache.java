package com.lebhas.creativesaas.redis;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RedisAiPromptCache {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final RedisCacheService redisCacheService;
    private final RedisKeyBuilder redisKeyBuilder;

    public RedisAiPromptCache(RedisCacheService redisCacheService, RedisKeyBuilder redisKeyBuilder) {
        this.redisCacheService = redisCacheService;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public void store(String promptHash, PromptCacheValue value) {
        store(promptHash, value, DEFAULT_TTL);
    }

    public void store(String promptHash, PromptCacheValue value, Duration ttl) {
        redisCacheService.set(redisKeyBuilder.promptHash(promptHash), value, ttl);
    }

    public Optional<PromptCacheValue> get(String promptHash) {
        return redisCacheService.get(redisKeyBuilder.promptHash(promptHash), PromptCacheValue.class);
    }

    public String hash(String prompt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(prompt.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is required", exception);
        }
    }

    public record PromptCacheValue(String promptHash, String provider, String model, String payload, Instant cachedAt) {
    }
}
