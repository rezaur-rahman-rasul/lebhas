package com.lebhas.creativesaas.profile.event;

import java.time.Instant;
import java.util.UUID;

public record ProfileImageChangedEventDto(
        UUID workspaceId,
        UUID profileId,
        UUID userId,
        UUID actorUserId,
        boolean removed,
        Instant occurredAt
) {
}
