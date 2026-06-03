package com.lebhas.asset.event;

import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.asset.domain.AssetType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AssetUploadedEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID assetId,
        UUID projectId,
        UUID uploadSessionId,
        AssetType assetType,
        AssetCategory assetCategory,
        AssetFileType fileType,
        String mimeType,
        long fileSize,
        String storageKey
) {

    public AssetUploadedEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        assetId = Objects.requireNonNull(assetId, "assetId must not be null");
        uploadSessionId = Objects.requireNonNull(uploadSessionId, "uploadSessionId must not be null");
        mimeType = normalizeNullable(mimeType);
        storageKey = normalizeNullable(storageKey);
        fileSize = Math.max(fileSize, 0L);
    }

    public static AssetUploadedEvent from(AssetEntity asset, UUID uploadSessionId) {
        return new AssetUploadedEvent(
                null,
                null,
                asset.getWorkspaceId(),
                asset.getId(),
                asset.getProjectId(),
                uploadSessionId,
                asset.getAssetType(),
                asset.getAssetCategory(),
                asset.getFileType(),
                asset.getMimeType(),
                asset.getFileSize(),
                asset.getStorageKey());
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
