package com.lebhas.creativesaas.prompt.cache;

import com.lebhas.creativesaas.pricing.cache.PricingRedisKeys;

import java.util.UUID;

public final class PromptRedisKeys {

    private static final String CREATIVE_REQUEST = "creative:request:%s";
    private static final String PROMPT_ENHANCED = "prompt:enhanced:%s";
    private static final String PROMPT_TEMPLATE = "prompt:template:%s";
    private static final String GENERATION_QUOTA = "generation:quota:%s";
    private static final String REQUEST_PROCESSING_LOCK = "request:processing-lock:%s";

    private PromptRedisKeys() {
    }

    public static String creativeRequest(UUID requestId) {
        return CREATIVE_REQUEST.formatted(require(requestId, "requestId"));
    }

    public static String promptEnhanced(String sha256) {
        return PROMPT_ENHANCED.formatted(requireText(sha256, "sha256"));
    }

    public static String workspaceSubscription(UUID workspaceId) {
        return PricingRedisKeys.workspaceSubscription(workspaceId);
    }

    public static String planFeatures(UUID planId) {
        return PricingRedisKeys.planFeatures(planId);
    }

    public static String promptTemplate(UUID templateId) {
        return PROMPT_TEMPLATE.formatted(require(templateId, "templateId"));
    }

    public static String generationQuota(UUID workspaceId) {
        return GENERATION_QUOTA.formatted(require(workspaceId, "workspaceId"));
    }

    public static String requestProcessingLock(UUID requestId) {
        return REQUEST_PROCESSING_LOCK.formatted(require(requestId, "requestId"));
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
