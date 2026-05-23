package com.lebhas.ai.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.ai.redis")
public class AiRedisCacheProperties {

    private Duration jobStateTtl = Duration.ofHours(12);
    private Duration progressTtl = Duration.ofHours(12);
    private Duration promptResponseTtl = Duration.ofHours(24);
    private Duration duplicateGenerationLockTtl = Duration.ofMinutes(10);
    private Duration creditReservationLockTtl = Duration.ofSeconds(30);
    private Duration retryThrottleWindow = Duration.ofMinutes(15);
    private int retryThrottleLimit = 5;
    private Duration providerRateLimitWindow = Duration.ofMinutes(1);
    private int providerRateLimit = 60;
    private Duration activePipelineTtl = Duration.ofMinutes(10);
    private Duration pipelineDefinitionTtl = Duration.ofMinutes(30);
    private Duration layerDefinitionTtl = Duration.ofMinutes(30);
    private Duration providerStatusTtl = Duration.ofMinutes(5);
    private Duration pipelineExecutionStateTtl = Duration.ofHours(12);
    private Duration layerExecutionStateTtl = Duration.ofHours(12);
    private Duration routingDecisionTtl = Duration.ofMinutes(5);
    private Duration fallbackStateTtl = Duration.ofHours(2);
    private Duration retryStateTtl = Duration.ofHours(2);
    private Duration generationLockTtl = Duration.ofMinutes(15);
    private Duration costEstimationTtl = Duration.ofMinutes(15);
    private Duration providerMetricsTtl = Duration.ofMinutes(5);
    private Duration providerHealthTtl = Duration.ofMinutes(2);
    private Duration layerAnalyticsTtl = Duration.ofMinutes(10);
    private Duration workspaceUsageTtl = Duration.ofMinutes(5);
    private Duration qualityScoreTtl = Duration.ofHours(1);
    private Duration failureRecentTtl = Duration.ofHours(2);
    private Duration costEstimateAnalyticsTtl = Duration.ofMinutes(15);
    private Duration routingRecommendationTtl = Duration.ofMinutes(5);

    public Duration getJobStateTtl() {
        return jobStateTtl;
    }

    public void setJobStateTtl(Duration jobStateTtl) {
        this.jobStateTtl = jobStateTtl;
    }

    public Duration getProgressTtl() {
        return progressTtl;
    }

    public void setProgressTtl(Duration progressTtl) {
        this.progressTtl = progressTtl;
    }

    public Duration getPromptResponseTtl() {
        return promptResponseTtl;
    }

    public void setPromptResponseTtl(Duration promptResponseTtl) {
        this.promptResponseTtl = promptResponseTtl;
    }

    public Duration getDuplicateGenerationLockTtl() {
        return duplicateGenerationLockTtl;
    }

    public void setDuplicateGenerationLockTtl(Duration duplicateGenerationLockTtl) {
        this.duplicateGenerationLockTtl = duplicateGenerationLockTtl;
    }

    public Duration getCreditReservationLockTtl() {
        return creditReservationLockTtl;
    }

    public void setCreditReservationLockTtl(Duration creditReservationLockTtl) {
        this.creditReservationLockTtl = creditReservationLockTtl;
    }

    public Duration getRetryThrottleWindow() {
        return retryThrottleWindow;
    }

    public void setRetryThrottleWindow(Duration retryThrottleWindow) {
        this.retryThrottleWindow = retryThrottleWindow;
    }

    public int getRetryThrottleLimit() {
        return retryThrottleLimit;
    }

    public void setRetryThrottleLimit(int retryThrottleLimit) {
        this.retryThrottleLimit = retryThrottleLimit;
    }

    public Duration getProviderRateLimitWindow() {
        return providerRateLimitWindow;
    }

    public void setProviderRateLimitWindow(Duration providerRateLimitWindow) {
        this.providerRateLimitWindow = providerRateLimitWindow;
    }

    public int getProviderRateLimit() {
        return providerRateLimit;
    }

    public void setProviderRateLimit(int providerRateLimit) {
        this.providerRateLimit = providerRateLimit;
    }

    public Duration getActivePipelineTtl() {
        return activePipelineTtl;
    }

    public void setActivePipelineTtl(Duration activePipelineTtl) {
        this.activePipelineTtl = activePipelineTtl;
    }

    public Duration getPipelineDefinitionTtl() {
        return pipelineDefinitionTtl;
    }

    public void setPipelineDefinitionTtl(Duration pipelineDefinitionTtl) {
        this.pipelineDefinitionTtl = pipelineDefinitionTtl;
    }

    public Duration getLayerDefinitionTtl() {
        return layerDefinitionTtl;
    }

    public void setLayerDefinitionTtl(Duration layerDefinitionTtl) {
        this.layerDefinitionTtl = layerDefinitionTtl;
    }

    public Duration getProviderStatusTtl() {
        return providerStatusTtl;
    }

    public void setProviderStatusTtl(Duration providerStatusTtl) {
        this.providerStatusTtl = providerStatusTtl;
    }

    public Duration getPipelineExecutionStateTtl() {
        return pipelineExecutionStateTtl;
    }

    public void setPipelineExecutionStateTtl(Duration pipelineExecutionStateTtl) {
        this.pipelineExecutionStateTtl = pipelineExecutionStateTtl;
    }

    public Duration getLayerExecutionStateTtl() {
        return layerExecutionStateTtl;
    }

    public void setLayerExecutionStateTtl(Duration layerExecutionStateTtl) {
        this.layerExecutionStateTtl = layerExecutionStateTtl;
    }

    public Duration getRoutingDecisionTtl() {
        return routingDecisionTtl;
    }

    public void setRoutingDecisionTtl(Duration routingDecisionTtl) {
        this.routingDecisionTtl = routingDecisionTtl;
    }

    public Duration getFallbackStateTtl() {
        return fallbackStateTtl;
    }

    public void setFallbackStateTtl(Duration fallbackStateTtl) {
        this.fallbackStateTtl = fallbackStateTtl;
    }

    public Duration getRetryStateTtl() {
        return retryStateTtl;
    }

    public void setRetryStateTtl(Duration retryStateTtl) {
        this.retryStateTtl = retryStateTtl;
    }

    public Duration getGenerationLockTtl() {
        return generationLockTtl;
    }

    public void setGenerationLockTtl(Duration generationLockTtl) {
        this.generationLockTtl = generationLockTtl;
    }

    public Duration getCostEstimationTtl() {
        return costEstimationTtl;
    }

    public void setCostEstimationTtl(Duration costEstimationTtl) {
        this.costEstimationTtl = costEstimationTtl;
    }

    public Duration getProviderMetricsTtl() {
        return providerMetricsTtl;
    }

    public void setProviderMetricsTtl(Duration providerMetricsTtl) {
        this.providerMetricsTtl = providerMetricsTtl;
    }

    public Duration getProviderHealthTtl() {
        return providerHealthTtl;
    }

    public void setProviderHealthTtl(Duration providerHealthTtl) {
        this.providerHealthTtl = providerHealthTtl;
    }

    public Duration getLayerAnalyticsTtl() {
        return layerAnalyticsTtl;
    }

    public void setLayerAnalyticsTtl(Duration layerAnalyticsTtl) {
        this.layerAnalyticsTtl = layerAnalyticsTtl;
    }

    public Duration getWorkspaceUsageTtl() {
        return workspaceUsageTtl;
    }

    public void setWorkspaceUsageTtl(Duration workspaceUsageTtl) {
        this.workspaceUsageTtl = workspaceUsageTtl;
    }

    public Duration getQualityScoreTtl() {
        return qualityScoreTtl;
    }

    public void setQualityScoreTtl(Duration qualityScoreTtl) {
        this.qualityScoreTtl = qualityScoreTtl;
    }

    public Duration getFailureRecentTtl() {
        return failureRecentTtl;
    }

    public void setFailureRecentTtl(Duration failureRecentTtl) {
        this.failureRecentTtl = failureRecentTtl;
    }

    public Duration getCostEstimateAnalyticsTtl() {
        return costEstimateAnalyticsTtl;
    }

    public void setCostEstimateAnalyticsTtl(Duration costEstimateAnalyticsTtl) {
        this.costEstimateAnalyticsTtl = costEstimateAnalyticsTtl;
    }

    public Duration getRoutingRecommendationTtl() {
        return routingRecommendationTtl;
    }

    public void setRoutingRecommendationTtl(Duration routingRecommendationTtl) {
        this.routingRecommendationTtl = routingRecommendationTtl;
    }
}
