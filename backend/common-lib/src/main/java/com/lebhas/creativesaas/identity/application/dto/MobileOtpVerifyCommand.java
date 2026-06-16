package com.lebhas.creativesaas.identity.application.dto;

public record MobileOtpVerifyCommand(
        String otpToken,
        String otp,
        String deviceId,
        String clientIp,
        String userAgent
) {
}
