package com.lebhas.creativesaas.profile.event;

import java.time.Instant;
import java.util.UUID;

public record ProfilePasswordChangedEventDto(
        UUID workspaceId,
        UUID userId,
        UUID actorUserId,
        boolean otherSessionsRevoked,
        int revokedDeviceCount,
        Instant occurredAt
) {
}
