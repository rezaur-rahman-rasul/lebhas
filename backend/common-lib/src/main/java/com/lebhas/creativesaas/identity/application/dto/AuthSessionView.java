package com.lebhas.creativesaas.identity.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthSessionView(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        String deviceId,
        UserView user,
        UUID workspaceId,
        UUID brandId,
        UUID productServiceId,
        UUID projectCampaignId,
        String nextStep
) {
}
