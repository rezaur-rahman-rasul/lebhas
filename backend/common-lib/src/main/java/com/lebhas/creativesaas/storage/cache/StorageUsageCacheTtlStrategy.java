package com.lebhas.creativesaas.storage.cache;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class StorageUsageCacheTtlStrategy {

    private static final Duration STORAGE_USAGE_TTL = Duration.ofMinutes(15);

    public Duration storageUsageTtl() {
        return STORAGE_USAGE_TTL;
    }
}
