package com.lebhas.creativesaas.sharing.application.dto;

import java.time.Instant;
import java.util.UUID;

public record RevisedShareLinkView(
        UUID id,
        UUID workspaceId,
        UUID generatedVersionId,
        String token,
        Instant expiresAt,
        long accessCount,
        UUID createdBy,
        Instant createdAt
) {
}
