package com.lebhas.creativesaas.storage.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StorageUsageUpdatedEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID storageUsageId,
        UUID assetId,
        long currentUsageBytes,
        Long storageLimitBytes,
        long rawAssetBytes,
        long generatedAssetBytes,
        long variantBytes,
        long deletedBytes,
        String reason,
        Instant lastCalculatedAt
) {

    public StorageUsageUpdatedEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        storageUsageId = Objects.requireNonNull(storageUsageId, "storageUsageId must not be null");
        currentUsageBytes = Math.max(currentUsageBytes, 0L);
        storageLimitBytes = storageLimitBytes == null ? null : Math.max(storageLimitBytes, 0L);
        rawAssetBytes = Math.max(rawAssetBytes, 0L);
        generatedAssetBytes = Math.max(generatedAssetBytes, 0L);
        variantBytes = Math.max(variantBytes, 0L);
        deletedBytes = Math.max(deletedBytes, 0L);
        reason = normalizeReason(reason, "STORAGE_USAGE_UPDATED");
    }

    private static String normalizeEventId(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value.trim();
    }

    private static String normalizeReason(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
