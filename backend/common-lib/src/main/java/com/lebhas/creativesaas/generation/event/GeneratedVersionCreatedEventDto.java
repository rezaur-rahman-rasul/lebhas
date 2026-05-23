package com.lebhas.creativesaas.generation.event;

import java.time.Instant;
import java.util.UUID;

public record GeneratedVersionCreatedEventDto(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID assetId,
        UUID storageFileId,
        int versionNumber,
        String generationStatus,
        Instant occurredAt
) {
    public GeneratedVersionCreatedEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
