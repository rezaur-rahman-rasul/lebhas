package com.lebhas.ai.config;

import com.lebhas.ai.cache.AiCreditReservationLockService;
import com.lebhas.ai.cache.AiDuplicateGenerationLockService;
import com.lebhas.ai.cache.AiCostEstimateCacheService;
import com.lebhas.ai.cache.AiCostEstimationCacheService;
import com.lebhas.ai.cache.AiFallbackStateCacheService;
import com.lebhas.ai.cache.AiFailureCacheService;
import com.lebhas.ai.cache.AiGenerationLockService;
import com.lebhas.ai.cache.AiGenerationProgressRedisService;
import com.lebhas.ai.cache.AiLayerAnalyticsCacheService;
import com.lebhas.ai.cache.AiLayerExecutionStateCacheService;
import com.lebhas.ai.cache.AiLayerMappingCacheService;
import com.lebhas.ai.cache.AiPipelineCacheService;
import com.lebhas.ai.cache.AiPipelineExecutionStateCacheService;
import com.lebhas.ai.cache.AiPromptResponseRedisCacheService;
import com.lebhas.ai.cache.AiProviderHealthCacheService;
import com.lebhas.ai.cache.AiProviderMetricsCacheService;
import com.lebhas.ai.cache.AiProviderOperationalCacheService;
import com.lebhas.ai.cache.AiProviderRateLimitStateService;
import com.lebhas.ai.cache.AiQualityScoreCacheService;
import com.lebhas.ai.cache.AiRedisAccessSupport;
import com.lebhas.ai.cache.AiRedisCacheProperties;
import com.lebhas.ai.cache.AiRedisTtlStrategy;
import com.lebhas.ai.cache.AiRetryStateCacheService;
import com.lebhas.ai.cache.AiRetryThrottleService;
import com.lebhas.ai.cache.AiRoutingDecisionCacheService;
import com.lebhas.ai.cache.AiRoutingRecommendationCacheService;
import com.lebhas.ai.cache.WorkspaceAiUsageCacheService;
import com.lebhas.ai.job.AiJobStateRedisService;
import com.lebhas.creativesaas.redis.RedisAiPromptCache;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisGenerationDeduplicationLock;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisRateLimitService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiRedisCacheProperties.class)
public class AiRedisFoundationConfiguration {

    @Bean
    AiRedisTtlStrategy aiRedisTtlStrategy(AiRedisCacheProperties properties) {
        return new AiRedisTtlStrategy(properties);
    }

    @Bean
    AiRedisAccessSupport aiRedisAccessSupport(
            RedisCacheService redisCacheService,
            RedisLockService redisLockService,
            RedisRateLimitService redisRateLimitService
    ) {
        return new AiRedisAccessSupport(redisCacheService, redisLockService, redisRateLimitService);
    }

    @Bean
    AiJobStateRedisService aiJobStateRedisService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiJobStateRedisService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiGenerationProgressRedisService aiGenerationProgressRedisService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiGenerationProgressRedisService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiPromptResponseRedisCacheService aiPromptResponseRedisCacheService(
            RedisAiPromptCache redisAiPromptCache,
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiPromptResponseRedisCacheService(redisAiPromptCache, redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiDuplicateGenerationLockService aiDuplicateGenerationLockService(
            RedisGenerationDeduplicationLock deduplicationLock,
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiDuplicateGenerationLockService(deduplicationLock, redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiCreditReservationLockService aiCreditReservationLockService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiCreditReservationLockService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiRetryThrottleService aiRetryThrottleService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiRetryThrottleService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiProviderRateLimitStateService aiProviderRateLimitStateService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiProviderRateLimitStateService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiPipelineCacheService aiPipelineCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiPipelineCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiLayerMappingCacheService aiLayerMappingCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiLayerMappingCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiProviderOperationalCacheService aiProviderOperationalCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiProviderOperationalCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiPipelineExecutionStateCacheService aiPipelineExecutionStateCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiPipelineExecutionStateCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiLayerExecutionStateCacheService aiLayerExecutionStateCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiLayerExecutionStateCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiRoutingDecisionCacheService aiRoutingDecisionCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiRoutingDecisionCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiFallbackStateCacheService aiFallbackStateCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiFallbackStateCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiRetryStateCacheService aiRetryStateCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiRetryStateCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiGenerationLockService aiGenerationLockService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiGenerationLockService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiCostEstimationCacheService aiCostEstimationCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiCostEstimationCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiProviderMetricsCacheService aiProviderMetricsCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiProviderMetricsCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiProviderHealthCacheService aiProviderHealthCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiProviderHealthCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiLayerAnalyticsCacheService aiLayerAnalyticsCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiLayerAnalyticsCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    WorkspaceAiUsageCacheService workspaceAiUsageCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new WorkspaceAiUsageCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiQualityScoreCacheService aiQualityScoreCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiQualityScoreCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiFailureCacheService aiFailureCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiFailureCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiCostEstimateCacheService aiCostEstimateCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiCostEstimateCacheService(redisAccessSupport, ttlStrategy);
    }

    @Bean
    AiRoutingRecommendationCacheService aiRoutingRecommendationCacheService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        return new AiRoutingRecommendationCacheService(redisAccessSupport, ttlStrategy);
    }
}
