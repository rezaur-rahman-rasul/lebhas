package com.lebhas.creativesaas.creative.interfaces;

import java.time.Instant;
import java.util.UUID;

public record PublicShareMetadataResponse(
        UUID generatedVersionId,
        UUID workspaceId,
        String versionName,
        int versionNumber,
        Instant expiresAt,
        boolean passwordProtected
) {
}
