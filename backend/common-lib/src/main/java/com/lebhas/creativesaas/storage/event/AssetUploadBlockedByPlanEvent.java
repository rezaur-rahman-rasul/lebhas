package com.lebhas.creativesaas.storage.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AssetUploadBlockedByPlanEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID assetId,
        UUID projectId,
        UUID pricingPlanId,
        UUID subscriptionId,
        String pricingPlanCode,
        String subscriptionStatus,
        String assetType,
        long currentUsageBytes,
        long incomingBytes,
        long projectedUsageBytes,
        Long storageLimitBytes,
        Long allowedUploadSizeBytes,
        String reason
) {

    public AssetUploadBlockedByPlanEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        pricingPlanCode = normalizeNullable(pricingPlanCode);
        subscriptionStatus = normalizeNullable(subscriptionStatus);
        assetType = normalizeNullable(assetType);
        currentUsageBytes = Math.max(currentUsageBytes, 0L);
        incomingBytes = Math.max(incomingBytes, 0L);
        projectedUsageBytes = Math.max(projectedUsageBytes, 0L);
        storageLimitBytes = normalizeNullableLong(storageLimitBytes);
        allowedUploadSizeBytes = normalizeNullableLong(allowedUploadSizeBytes);
        reason = normalizeReason(reason, "ASSET_UPLOAD_BLOCKED_BY_PLAN");
    }

    private static String normalizeEventId(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value.trim();
    }

    private static Long normalizeNullableLong(Long value) {
        if (value == null) {
            return null;
        }
        return Math.max(value, 0L);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeReason(String value, String fallback) {
        String normalized = normalizeNullable(value);
        return normalized == null ? fallback : normalized;
    }
}
