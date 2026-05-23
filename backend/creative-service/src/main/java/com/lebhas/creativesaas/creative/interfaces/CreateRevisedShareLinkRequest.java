package com.lebhas.creativesaas.creative.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Creates a share link for an approved generated version.")
public record CreateRevisedShareLinkRequest(
        @NotNull
        @Schema(description = "Generated version identifier", format = "uuid")
        UUID generatedVersionId,
        @Size(max = 120)
        @Schema(description = "Optional caller-provided token. When omitted, a unique token is generated.", maxLength = 120)
        String token,
        @NotNull
        @Schema(description = "Absolute expiration timestamp in ISO-8601 format")
        Instant expiresAt
) {
}
