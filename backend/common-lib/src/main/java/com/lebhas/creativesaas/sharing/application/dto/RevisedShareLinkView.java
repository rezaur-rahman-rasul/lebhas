package com.lebhas.creativesaas.sharing.application.dto;

import java.time.Instant;
import java.util.UUID;

public record RevisedShareLinkView(
        UUID id,
        UUID workspaceId,
        UUID generatedVersionId,
        String token,
        String tokenHash,
        Instant expiresAt,
        long accessCount,
        boolean revoked,
        Instant revokedAt,
        UUID createdBy,
        Instant createdAt
) {
}
