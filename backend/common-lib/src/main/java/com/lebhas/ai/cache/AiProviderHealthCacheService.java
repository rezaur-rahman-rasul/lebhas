package com.lebhas.ai.cache;

import com.lebhas.ai.application.dto.ProviderHealthSnapshot;

import java.util.Optional;
import java.util.UUID;

public class AiProviderHealthCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiProviderHealthCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<ProviderHealthSnapshot> get(UUID providerId) {
        return redisAccessSupport.read(
                AiAnalyticsRedisKeys.providerHealth(providerId),
                ProviderHealthSnapshot.class,
                "ai-provider-health-cache-read",
                new AiRedisOperationContext(null, null, null, providerId == null ? null : providerId.toString()));
    }

    public boolean store(ProviderHealthSnapshot snapshot) {
        return redisAccessSupport.write(
                AiAnalyticsRedisKeys.providerHealth(snapshot.providerId()),
                snapshot,
                ttlStrategy.providerHealthTtl(),
                "ai-provider-health-cache-write",
                new AiRedisOperationContext(null, null, null, snapshot.providerId() == null ? null : snapshot.providerId().toString()));
    }

    public boolean invalidate(UUID providerId) {
        return redisAccessSupport.delete(
                AiAnalyticsRedisKeys.providerHealth(providerId),
                "ai-provider-health-cache-delete",
                new AiRedisOperationContext(null, null, null, providerId == null ? null : providerId.toString()));
    }
}
