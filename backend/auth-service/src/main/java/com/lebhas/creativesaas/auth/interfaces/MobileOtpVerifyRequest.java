package com.lebhas.creativesaas.auth.interfaces;

import jakarta.validation.constraints.NotBlank;

public record MobileOtpVerifyRequest(
        @NotBlank String otpToken,
        @NotBlank String otp,
        String deviceId
) {
}
