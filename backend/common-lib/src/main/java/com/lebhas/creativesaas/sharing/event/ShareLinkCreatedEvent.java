package com.lebhas.creativesaas.sharing.event;

import java.time.Instant;
import java.util.UUID;

public record ShareLinkCreatedEvent(
        UUID shareLinkId,
        UUID workspaceId,
        UUID generatedVersionId,
        String token,
        UUID createdBy,
        Instant expiresAt,
        Instant occurredAt
) {
}
