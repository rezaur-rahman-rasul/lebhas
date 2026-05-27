package com.lebhas.creativesaas.profile.application.dto;

import java.time.Instant;
import java.util.UUID;

public record UserSessionView(
        String sessionId,
        UUID workspaceId,
        String deviceId,
        String maskedIpAddress,
        String userAgent,
        boolean current,
        boolean active,
        Instant lastUsedAt,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt
) {
}
