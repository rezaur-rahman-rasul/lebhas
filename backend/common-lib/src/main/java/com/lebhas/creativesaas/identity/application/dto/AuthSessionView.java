package com.lebhas.creativesaas.identity.application.dto;

import java.time.Instant;

public record AuthSessionView(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        String deviceId,
        UserView user
) {
}
