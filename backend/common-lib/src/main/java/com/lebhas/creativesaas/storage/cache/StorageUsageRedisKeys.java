package com.lebhas.creativesaas.storage.cache;

import java.util.UUID;

public final class StorageUsageRedisKeys {

    private static final String STORAGE_USAGE = "storage:usage:%s";

    private StorageUsageRedisKeys() {
    }

    public static String storageUsage(UUID workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        return STORAGE_USAGE.formatted(workspaceId);
    }
}
