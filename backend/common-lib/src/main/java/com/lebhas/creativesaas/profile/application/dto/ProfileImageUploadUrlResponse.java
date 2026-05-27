package com.lebhas.creativesaas.profile.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ProfileImageUploadUrlResponse(
        UUID uploadReferenceId,
        String uploadUrl,
        Instant expiresAt,
        long maxFileSize,
        String requiredMethod
) {
}
