package com.lebhas.asset.event;

import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.asset.domain.AssetVariantType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AssetVariantGenerateEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID assetId,
        UUID projectId,
        AssetVariantType variantType,
        AssetFileType fileType,
        String mimeType,
        String sourceStorageKey
) {

    public AssetVariantGenerateEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        assetId = Objects.requireNonNull(assetId, "assetId must not be null");
        projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        variantType = variantType == null ? AssetVariantType.THUMBNAIL : variantType;
        mimeType = normalizeNullable(mimeType);
        sourceStorageKey = normalizeNullable(sourceStorageKey);
    }

    public static AssetVariantGenerateEvent thumbnailFrom(AssetProcessEvent event) {
        return new AssetVariantGenerateEvent(
                null,
                null,
                event.workspaceId(),
                event.assetId(),
                event.projectId(),
                AssetVariantType.THUMBNAIL,
                event.fileType(),
                event.mimeType(),
                event.storageKey());
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
