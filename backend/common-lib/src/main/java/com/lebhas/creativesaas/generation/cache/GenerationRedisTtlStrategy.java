package com.lebhas.creativesaas.generation.cache;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class GenerationRedisTtlStrategy {

    private final GenerationRedisCacheProperties properties;

    public GenerationRedisTtlStrategy(GenerationRedisCacheProperties properties) {
        this.properties = properties;
    }

    public Duration generationJobTtl() {
        return normalize(properties.getGenerationJobTtl(), Duration.ofHours(2));
    }

    public Duration generationLockTtl() {
        return normalize(properties.getGenerationLockTtl(), Duration.ofSeconds(45));
    }

    public Duration generatedVersionCountTtl() {
        return normalize(properties.getGeneratedVersionCountTtl(), Duration.ofMinutes(15));
    }

    public Duration workspaceQuotaTtl() {
        return normalize(properties.getWorkspaceQuotaTtl(), Duration.ofMinutes(10));
    }

    public Duration creditReservationTtl() {
        return normalize(properties.getCreditReservationTtl(), Duration.ofMinutes(30));
    }

    public Duration providerRateLimitWindow() {
        return normalize(properties.getProviderRateLimitWindow(), Duration.ofMinutes(1));
    }

    public Duration providerRateLimitWindow(Duration override) {
        return normalize(override, providerRateLimitWindow());
    }

    private Duration normalize(Duration candidate, Duration fallback) {
        if (candidate == null || candidate.isNegative() || candidate.isZero()) {
            return fallback;
        }
        return candidate;
    }
}
