package com.lebhas.creativesaas.sharing.application.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateRevisedShareLinkCommand(
        UUID workspaceId,
        UUID generatedVersionId,
        String token,
        Instant expiresAt
) {
}
