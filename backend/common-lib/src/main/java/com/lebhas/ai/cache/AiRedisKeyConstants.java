package com.lebhas.ai.cache;

import java.util.Locale;
import java.util.UUID;

public final class AiRedisKeyConstants {

    public static final String AI_JOB_PREFIX = "ai:job:";
    public static final String AI_PROGRESS_PREFIX = "ai:progress:";
    public static final String PROMPT_HASH_PREFIX = "prompt:hash:";
    public static final String GENERATION_LOCK_PREFIX = "generation:lock:";
    public static final String CREDIT_RESERVATION_LOCK_PREFIX = "lock:wallet:";
    public static final String RETRY_CREATIVE_REQUEST_PREFIX = "retry:creative-request:";
    public static final String PROVIDER_RATE_PREFIX = "ai:provider:rate:";
    public static final String ACTIVE_PIPELINE_KEY = "ai:pipeline:active";
    public static final String PIPELINE_PREFIX = "ai:pipeline:";
    public static final String LAYER_PREFIX = "ai:layer:";
    public static final String LAYER_MAPPING_PREFIX = "ai:layer:mapping:";
    public static final String PROVIDER_PREFIX = "ai:provider:";
    public static final String ROUTING_PREFIX = "ai:routing:";
    public static final String GENERATION_STATE_PREFIX = "ai:generation:state:";
    public static final String LAYER_STATE_PREFIX = "ai:layer:state:";
    public static final String PIPELINE_GENERATION_LOCK_PREFIX = "ai:generation:lock:";
    public static final String FALLBACK_STATE_PREFIX = "ai:fallback:";
    public static final String RETRY_STATE_PREFIX = "ai:retry:";
    public static final String COST_ESTIMATION_PREFIX = "ai:cost:";

    private AiRedisKeyConstants() {
    }

    public static String aiJob(UUID jobId) {
        return AI_JOB_PREFIX + normalizeUuid(jobId);
    }

    public static String aiProgress(UUID creativeRequestId) {
        return AI_PROGRESS_PREFIX + normalizeUuid(creativeRequestId);
    }

    public static String promptHash(String sha256) {
        return PROMPT_HASH_PREFIX + normalizeSegment(sha256);
    }

    public static String generationLock(String requestHash) {
        return GENERATION_LOCK_PREFIX + normalizeSegment(requestHash);
    }

    public static String creditReservationLock(UUID workspaceId) {
        return CREDIT_RESERVATION_LOCK_PREFIX + normalizeUuid(workspaceId);
    }

    public static String retryCreativeRequest(UUID creativeRequestId) {
        return RETRY_CREATIVE_REQUEST_PREFIX + normalizeUuid(creativeRequestId);
    }

    public static String providerRate(String provider, UUID workspaceId) {
        return PROVIDER_RATE_PREFIX + normalizeSegment(provider).toLowerCase(Locale.ROOT) + ":" + normalizeUuid(workspaceId);
    }

    public static String activePipeline() {
        return ACTIVE_PIPELINE_KEY;
    }

    public static String pipeline(UUID pipelineId) {
        return PIPELINE_PREFIX + normalizeUuid(pipelineId);
    }

    public static String layer(UUID layerId) {
        return LAYER_PREFIX + normalizeUuid(layerId);
    }

    public static String layerMapping(UUID layerId) {
        return LAYER_MAPPING_PREFIX + normalizeUuid(layerId);
    }

    public static String provider(UUID providerId) {
        return PROVIDER_PREFIX + normalizeUuid(providerId);
    }

    public static String providerRate(UUID providerId, UUID workspaceId) {
        return PROVIDER_RATE_PREFIX + normalizeUuid(providerId) + ":" + normalizeUuid(workspaceId);
    }

    public static String routing(UUID workspaceId, String layerType) {
        return ROUTING_PREFIX + normalizeUuid(workspaceId) + ":" + normalizeSegment(layerType).toUpperCase(Locale.ROOT);
    }

    public static String generationState(UUID creativeRequestId) {
        return GENERATION_STATE_PREFIX + normalizeUuid(creativeRequestId);
    }

    public static String layerState(UUID creativeRequestId, String layerType) {
        return LAYER_STATE_PREFIX + normalizeUuid(creativeRequestId) + ":" + normalizeSegment(layerType).toUpperCase(Locale.ROOT);
    }

    public static String pipelineGenerationLock(UUID creativeRequestId) {
        return PIPELINE_GENERATION_LOCK_PREFIX + normalizeUuid(creativeRequestId);
    }

    public static String fallbackState(UUID creativeRequestId, String layerType) {
        return FALLBACK_STATE_PREFIX + normalizeUuid(creativeRequestId) + ":" + normalizeSegment(layerType).toUpperCase(Locale.ROOT);
    }

    public static String retryState(UUID creativeRequestId, String layerType) {
        return RETRY_STATE_PREFIX + normalizeUuid(creativeRequestId) + ":" + normalizeSegment(layerType).toUpperCase(Locale.ROOT);
    }

    public static String costEstimation(UUID workspaceId, UUID creativeRequestId) {
        return COST_ESTIMATION_PREFIX + normalizeUuid(workspaceId) + ":" + normalizeUuid(creativeRequestId);
    }

    private static String normalizeUuid(UUID value) {
        return value == null ? "unknown" : value.toString();
    }

    private static String normalizeSegment(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }
}
