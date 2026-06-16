package com.lebhas.creativesaas.auth.interfaces;

public record LogoutRequest(
        String refreshToken,
        Boolean logoutAllDevices
) {
    public boolean shouldLogoutAllDevices() {
        return Boolean.TRUE.equals(logoutAllDevices);
    }
}
