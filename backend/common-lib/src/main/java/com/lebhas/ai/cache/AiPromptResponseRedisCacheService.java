package com.lebhas.ai.cache;

import com.lebhas.creativesaas.redis.RedisAiPromptCache;

import java.time.Instant;
import java.util.Optional;

public class AiPromptResponseRedisCacheService {

    private final RedisAiPromptCache redisAiPromptCache;
    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiPromptResponseRedisCacheService(
            RedisAiPromptCache redisAiPromptCache,
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        this.redisAiPromptCache = redisAiPromptCache;
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public String hash(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return redisAiPromptCache.hash("blank-prompt");
        }
        return redisAiPromptCache.hash(prompt.trim());
    }

    public Optional<PromptResponseCacheEntry> get(String promptHash) {
        AiRedisOperationContext context = AiRedisOperationContext.request(null, null);
        try {
            return redisAiPromptCache.get(promptHash)
                    .map(value -> new PromptResponseCacheEntry(
                            value.promptHash(),
                            value.provider(),
                            value.model(),
                            value.payload(),
                            value.cachedAt()));
        } catch (RuntimeException exception) {
            return redisAccessSupport.read(
                    AiRedisKeyConstants.promptHash(promptHash),
                    PromptResponseCacheEntry.class,
                    "prompt-cache-read-fallback",
                    context);
        }
    }

    public boolean store(PromptResponseCacheEntry entry) {
        AiRedisOperationContext context = AiRedisOperationContext.request(null, null);
        try {
            redisAiPromptCache.store(
                    entry.promptHash(),
                    new RedisAiPromptCache.PromptCacheValue(
                            entry.promptHash(),
                            entry.provider(),
                            entry.model(),
                            entry.payload(),
                            entry.cachedAt() == null ? Instant.now() : entry.cachedAt()),
                    ttlStrategy.promptResponseTtl());
            return true;
        } catch (RuntimeException exception) {
            return redisAccessSupport.write(
                    AiRedisKeyConstants.promptHash(entry.promptHash()),
                    entry,
                    ttlStrategy.promptResponseTtl(),
                    "prompt-cache-write-fallback",
                    context);
        }
    }
}
