package com.lebhas.creativesaas.identity.application.dto;

public record MobileOtpStartView(
        String otpToken,
        String mobileNumberMasked,
        boolean isNewUser,
        int resendAfterSeconds,
        int otpLength,
        boolean authenticated,
        AuthSessionView session
) {
}
