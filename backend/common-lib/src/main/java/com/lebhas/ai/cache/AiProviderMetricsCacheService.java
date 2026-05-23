package com.lebhas.ai.cache;

import com.lebhas.ai.application.dto.ProviderMetricsSnapshot;

import java.util.Optional;
import java.util.UUID;

public class AiProviderMetricsCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiProviderMetricsCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<ProviderMetricsSnapshot> get(UUID providerId, String modelName) {
        return redisAccessSupport.read(
                AiAnalyticsRedisKeys.providerMetrics(providerId, modelName),
                ProviderMetricsSnapshot.class,
                "ai-provider-metrics-cache-read",
                new AiRedisOperationContext(null, null, null, providerId == null ? null : providerId.toString()));
    }

    public boolean store(ProviderMetricsSnapshot snapshot) {
        return redisAccessSupport.write(
                AiAnalyticsRedisKeys.providerMetrics(snapshot.providerId(), snapshot.modelName()),
                snapshot,
                ttlStrategy.providerMetricsTtl(),
                "ai-provider-metrics-cache-write",
                new AiRedisOperationContext(null, null, null, snapshot.providerId() == null ? null : snapshot.providerId().toString()));
    }

    public boolean invalidate(UUID providerId, String modelName) {
        return redisAccessSupport.delete(
                AiAnalyticsRedisKeys.providerMetrics(providerId, modelName),
                "ai-provider-metrics-cache-delete",
                new AiRedisOperationContext(null, null, null, providerId == null ? null : providerId.toString()));
    }
}
