package com.lebhas.creativesaas.profile.event;

import java.time.Instant;
import java.util.UUID;

public record ProfileUpdatedEventDto(
        UUID workspaceId,
        UUID profileId,
        UUID userId,
        UUID actorUserId,
        Instant occurredAt
) {
}
