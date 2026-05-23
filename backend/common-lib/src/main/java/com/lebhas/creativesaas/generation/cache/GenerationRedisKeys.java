package com.lebhas.creativesaas.generation.cache;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class GenerationRedisKeys {

    public String generationJob(UUID jobId) {
        return "generation:job:" + jobId;
    }

    public String generationLock(UUID creativeRequestId) {
        return "generation:lock:" + creativeRequestId;
    }

    public String generatedVersions(UUID creativeRequestId) {
        return "generated:versions:" + creativeRequestId;
    }

    public String workspaceQuota(UUID workspaceId) {
        return "workspace:quota:" + workspaceId;
    }

    public String creditReservation(UUID creativeRequestId) {
        return "credit:reservation:" + creativeRequestId;
    }

    public String providerRateLimit(String provider) {
        return "provider:rate-limit:" + normalizeProvider(provider);
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "unknown";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
