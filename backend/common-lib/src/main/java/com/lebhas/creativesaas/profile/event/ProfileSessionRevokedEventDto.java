package com.lebhas.creativesaas.profile.event;

import java.time.Instant;
import java.util.UUID;

public record ProfileSessionRevokedEventDto(
        UUID workspaceId,
        UUID userId,
        UUID actorUserId,
        int revokedTokenCount,
        int revokedDeviceCount,
        boolean currentSessionIncluded,
        Instant occurredAt
) {
}
