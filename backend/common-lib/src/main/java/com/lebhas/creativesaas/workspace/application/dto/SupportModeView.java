package com.lebhas.creativesaas.workspace.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SupportModeView(
        UUID masterUserId,
        UUID workspaceId,
        String deviceId,
        boolean active,
        Instant startedAt,
        Instant expiresAt
) {
}
