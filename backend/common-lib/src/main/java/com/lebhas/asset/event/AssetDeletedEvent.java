package com.lebhas.asset.event;

import com.lebhas.creativesaas.asset.domain.AssetEntity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AssetDeletedEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID assetId,
        UUID projectId,
        UUID storageFileId,
        String storageKey,
        boolean storageReleased
) {

    public AssetDeletedEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        assetId = Objects.requireNonNull(assetId, "assetId must not be null");
        projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        storageKey = normalizeNullable(storageKey);
    }

    public static AssetDeletedEvent from(AssetEntity asset, boolean storageReleased) {
        return new AssetDeletedEvent(
                null,
                null,
                asset.getWorkspaceId(),
                asset.getId(),
                asset.getProjectId(),
                asset.getStorageFileId(),
                asset.getStorageKey(),
                storageReleased);
    }

    private static String normalizeEventId(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
