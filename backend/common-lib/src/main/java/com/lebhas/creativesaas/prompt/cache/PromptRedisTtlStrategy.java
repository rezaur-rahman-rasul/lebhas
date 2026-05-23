package com.lebhas.creativesaas.prompt.cache;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PromptRedisTtlStrategy {

    private final PromptRedisCacheProperties properties;

    public PromptRedisTtlStrategy(PromptRedisCacheProperties properties) {
        this.properties = properties;
    }

    public Duration creativeRequestTtl() {
        return normalize(properties.getCreativeRequestTtl(), Duration.ofMinutes(20));
    }

    public Duration promptEnhancementTtl() {
        return normalize(properties.getPromptEnhancementTtl(), Duration.ofHours(24));
    }

    public Duration promptTemplateTtl() {
        return normalize(properties.getPromptTemplateTtl(), Duration.ofMinutes(30));
    }

    public Duration generationQuotaTtl() {
        return normalize(properties.getGenerationQuotaTtl(), Duration.ofMinutes(10));
    }

    public Duration requestProcessingLockTtl() {
        return normalize(properties.getRequestProcessingLockTtl(), Duration.ofMinutes(2));
    }

    private Duration normalize(Duration candidate, Duration fallback) {
        if (candidate == null || candidate.isNegative() || candidate.isZero()) {
            return fallback;
        }
        return candidate;
    }
}
