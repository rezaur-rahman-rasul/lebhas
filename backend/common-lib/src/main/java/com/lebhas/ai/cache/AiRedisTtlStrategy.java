package com.lebhas.ai.cache;

import java.time.Duration;

public class AiRedisTtlStrategy {

    private final AiRedisCacheProperties properties;

    public AiRedisTtlStrategy(AiRedisCacheProperties properties) {
        this.properties = properties;
    }

    public Duration aiJobStateTtl() {
        return normalize(properties.getJobStateTtl(), Duration.ofHours(12));
    }

    public Duration generationProgressTtl() {
        return normalize(properties.getProgressTtl(), Duration.ofHours(12));
    }

    public Duration promptResponseTtl() {
        return normalize(properties.getPromptResponseTtl(), Duration.ofHours(24));
    }

    public Duration duplicateGenerationLockTtl() {
        return normalize(properties.getDuplicateGenerationLockTtl(), Duration.ofMinutes(10));
    }

    public Duration creditReservationLockTtl() {
        return normalize(properties.getCreditReservationLockTtl(), Duration.ofSeconds(30));
    }

    public Duration retryThrottleWindow(Duration override) {
        return normalize(override, normalize(properties.getRetryThrottleWindow(), Duration.ofMinutes(15)));
    }

    public int retryThrottleLimit(int override) {
        return override > 0 ? override : Math.max(1, properties.getRetryThrottleLimit());
    }

    public Duration providerRateLimitWindow(Duration override) {
        return normalize(override, normalize(properties.getProviderRateLimitWindow(), Duration.ofMinutes(1)));
    }

    public int providerRateLimit(int override) {
        return override > 0 ? override : Math.max(1, properties.getProviderRateLimit());
    }

    public Duration activePipelineTtl() {
        return normalize(properties.getActivePipelineTtl(), Duration.ofMinutes(10));
    }

    public Duration pipelineDefinitionTtl() {
        return normalize(properties.getPipelineDefinitionTtl(), Duration.ofMinutes(30));
    }

    public Duration layerDefinitionTtl() {
        return normalize(properties.getLayerDefinitionTtl(), Duration.ofMinutes(30));
    }

    public Duration providerStatusTtl() {
        return normalize(properties.getProviderStatusTtl(), Duration.ofMinutes(5));
    }

    public Duration pipelineExecutionStateTtl() {
        return normalize(properties.getPipelineExecutionStateTtl(), Duration.ofHours(12));
    }

    public Duration layerExecutionStateTtl() {
        return normalize(properties.getLayerExecutionStateTtl(), Duration.ofHours(12));
    }

    public Duration routingDecisionTtl() {
        return normalize(properties.getRoutingDecisionTtl(), Duration.ofMinutes(5));
    }

    public Duration fallbackStateTtl() {
        return normalize(properties.getFallbackStateTtl(), Duration.ofHours(2));
    }

    public Duration retryStateTtl() {
        return normalize(properties.getRetryStateTtl(), Duration.ofHours(2));
    }

    public Duration generationLockTtl() {
        return normalize(properties.getGenerationLockTtl(), Duration.ofMinutes(15));
    }

    public Duration costEstimationTtl() {
        return normalize(properties.getCostEstimationTtl(), Duration.ofMinutes(15));
    }

    public Duration providerMetricsTtl() {
        return normalize(properties.getProviderMetricsTtl(), Duration.ofMinutes(5));
    }

    public Duration providerHealthTtl() {
        return normalize(properties.getProviderHealthTtl(), Duration.ofMinutes(2));
    }

    public Duration layerAnalyticsTtl() {
        return normalize(properties.getLayerAnalyticsTtl(), Duration.ofMinutes(10));
    }

    public Duration workspaceUsageTtl() {
        return normalize(properties.getWorkspaceUsageTtl(), Duration.ofMinutes(5));
    }

    public Duration qualityScoreTtl() {
        return normalize(properties.getQualityScoreTtl(), Duration.ofHours(1));
    }

    public Duration failureRecentTtl() {
        return normalize(properties.getFailureRecentTtl(), Duration.ofHours(2));
    }

    public Duration costEstimateAnalyticsTtl() {
        return normalize(properties.getCostEstimateAnalyticsTtl(), costEstimationTtl());
    }

    public Duration routingRecommendationTtl() {
        return normalize(properties.getRoutingRecommendationTtl(), Duration.ofMinutes(5));
    }

    private Duration normalize(Duration candidate, Duration fallback) {
        if (candidate == null || candidate.isNegative() || candidate.isZero()) {
            return fallback;
        }
        return candidate;
    }
}
