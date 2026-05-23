package com.lebhas.creativesaas.identity.application.dto;

public record LogoutCommand(
        String refreshToken,
        boolean logoutAllDevices
) {
}
