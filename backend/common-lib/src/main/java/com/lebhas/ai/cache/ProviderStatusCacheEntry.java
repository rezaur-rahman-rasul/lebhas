package com.lebhas.ai.cache;

import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.domain.ProviderType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProviderStatusCacheEntry(
        UUID providerId,
        String providerCode,
        ProviderType providerType,
        ProviderStatus status,
        boolean enabled,
        boolean fallbackEligible,
        boolean workspaceRoutingEligible,
        boolean planRoutingEligible,
        Map<String, Object> rateLimitMetadata,
        Instant cachedAt
) {
    public ProviderStatusCacheEntry {
        rateLimitMetadata = rateLimitMetadata == null ? Map.of() : Map.copyOf(rateLimitMetadata);
        cachedAt = cachedAt == null ? Instant.now() : cachedAt;
    }
}
