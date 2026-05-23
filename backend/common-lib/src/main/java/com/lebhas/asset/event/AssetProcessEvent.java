package com.lebhas.asset.event;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetFileType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AssetProcessEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID assetId,
        UUID projectId,
        AssetFileType fileType,
        String mimeType,
        String storageKey,
        boolean imageOptimizationRequested,
        boolean thumbnailGenerationRequested,
        boolean metadataExtractionRequested,
        boolean aiPreprocessingRequested
) {

    public AssetProcessEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        assetId = Objects.requireNonNull(assetId, "assetId must not be null");
        projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        mimeType = normalizeNullable(mimeType);
        storageKey = normalizeNullable(storageKey);
    }

    public static AssetProcessEvent from(AssetEntity asset) {
        boolean imageLike = asset.getFileType() == AssetFileType.IMAGE
                || asset.getFileType() == AssetFileType.VECTOR_IMAGE;
        return new AssetProcessEvent(
                null,
                null,
                asset.getWorkspaceId(),
                asset.getId(),
                asset.getProjectId(),
                asset.getFileType(),
                asset.getMimeType(),
                asset.getStorageKey(),
                imageLike,
                imageLike,
                true,
                false);
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
