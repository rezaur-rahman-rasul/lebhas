package com.lebhas.creativesaas.identity.application.dto;

public record RefreshSessionCommand(
        String refreshToken,
        String deviceId,
        String clientIp,
        String userAgent
) {
}
