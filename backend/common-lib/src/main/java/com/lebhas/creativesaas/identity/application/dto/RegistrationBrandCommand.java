package com.lebhas.creativesaas.identity.application.dto;

public record RegistrationBrandCommand(String registrationSessionToken, String brandName, String deviceId, String clientIp, String userAgent) {
}
