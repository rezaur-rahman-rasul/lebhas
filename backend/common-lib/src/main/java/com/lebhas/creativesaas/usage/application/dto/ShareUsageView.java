package com.lebhas.creativesaas.usage.application.dto;

import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;

import java.time.Instant;
import java.util.UUID;

public record ShareUsageView(
        UUID id,
        UUID workspaceId,
        UUID shareLinkId,
        UUID generatedVersionId,
        UUID accessedByUserId,
        SafeProfileDisplayView accessedByDisplay,
        String accessIp,
        String userAgent,
        String referrer,
        long accessCount,
        Instant createdAt
) {
}
