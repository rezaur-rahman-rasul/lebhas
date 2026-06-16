package com.lebhas.creativesaas.identity.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MobileOtpVerifyView(
        String accessToken,
        java.time.Instant accessTokenExpiresAt,
        String refreshToken,
        java.time.Instant refreshTokenExpiresAt,
        String deviceId,
        UserView user,
        UUID workspaceId,
        boolean isNewUser,
        BigDecimal freeCreditsGranted
) {
}
