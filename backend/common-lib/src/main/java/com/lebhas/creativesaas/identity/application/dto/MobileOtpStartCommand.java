package com.lebhas.creativesaas.identity.application.dto;

public record MobileOtpStartCommand(
        String mobileNumber,
        String clientIp,
        String userAgent
) {
}
