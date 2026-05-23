package com.lebhas.ai.cache;

import java.util.UUID;

public final class AiAnalyticsRedisKeys {

    private AiAnalyticsRedisKeys() {
    }

    public static String providerMetrics(UUID providerId, String modelName) {
        return "ai:provider:metrics:" + normalizeUuid(providerId) + ":" + normalizeSegment(modelName);
    }

    public static String providerHealth(UUID providerId) {
        return "ai:provider:health:" + normalizeUuid(providerId);
    }

    public static String layerAnalytics(UUID layerId, UUID providerId) {
        return "ai:layer:analytics:" + normalizeUuid(layerId) + ":" + normalizeUuid(providerId);
    }

    public static String workspaceUsage(UUID workspaceId) {
        return "ai:workspace:usage:" + normalizeUuid(workspaceId);
    }

    public static String qualityScore(UUID generatedVersionId) {
        return "ai:quality:score:" + normalizeUuid(generatedVersionId);
    }

    public static String recentFailure(UUID providerId) {
        return "ai:failure:recent:" + normalizeUuid(providerId);
    }

    public static String costEstimate(UUID workspaceId, String requestHash) {
        return "ai:cost:estimate:" + normalizeUuid(workspaceId) + ":" + normalizeSegment(requestHash);
    }

    public static String routingRecommendation(UUID workspaceId, UUID layerId) {
        return "ai:routing:recommendation:" + normalizeUuid(workspaceId) + ":" + normalizeUuid(layerId);
    }

    private static String normalizeUuid(UUID value) {
        return value == null ? "unknown" : value.toString();
    }

    private static String normalizeSegment(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim()
                .replace(':', '_')
                .replaceAll("\\s+", "_");
    }
}
