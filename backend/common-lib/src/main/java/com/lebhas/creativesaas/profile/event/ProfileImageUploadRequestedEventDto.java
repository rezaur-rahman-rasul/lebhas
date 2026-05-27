package com.lebhas.creativesaas.profile.event;

import java.time.Instant;
import java.util.UUID;

public record ProfileImageUploadRequestedEventDto(
        UUID workspaceId,
        UUID uploadReferenceId,
        UUID userId,
        UUID actorUserId,
        String mimeType,
        long fileSize,
        String extension,
        Instant uploadUrlExpiresAt,
        Instant occurredAt
) {
}
