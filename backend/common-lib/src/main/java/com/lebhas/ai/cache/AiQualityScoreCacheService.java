package com.lebhas.ai.cache;

import com.lebhas.ai.application.dto.QualityScoreResult;

import java.util.Optional;
import java.util.UUID;

public class AiQualityScoreCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiQualityScoreCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<QualityScoreResult> get(UUID generatedVersionId) {
        return redisAccessSupport.read(
                AiAnalyticsRedisKeys.qualityScore(generatedVersionId),
                QualityScoreResult.class,
                "ai-quality-score-cache-read",
                null);
    }

    public boolean store(QualityScoreResult result) {
        return redisAccessSupport.write(
                AiAnalyticsRedisKeys.qualityScore(result.generatedVersionId()),
                result,
                ttlStrategy.qualityScoreTtl(),
                "ai-quality-score-cache-write",
                new AiRedisOperationContext(result.workspaceId(), null, null, null));
    }

    public boolean invalidate(UUID generatedVersionId) {
        return redisAccessSupport.delete(
                AiAnalyticsRedisKeys.qualityScore(generatedVersionId),
                "ai-quality-score-cache-delete",
                null);
    }
}
