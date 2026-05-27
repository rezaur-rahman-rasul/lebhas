package com.lebhas.creativesaas.profile.event;

import java.time.Instant;
import java.util.UUID;

public record ProfileSettingsUpdatedEventDto(
        UUID workspaceId,
        UUID settingsId,
        UUID userId,
        UUID actorUserId,
        Instant occurredAt
) {
}
