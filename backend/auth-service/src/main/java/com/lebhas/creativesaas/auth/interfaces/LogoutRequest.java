package com.lebhas.creativesaas.auth.interfaces;

public record LogoutRequest(
        String refreshToken,
        boolean logoutAllDevices
) {
}
