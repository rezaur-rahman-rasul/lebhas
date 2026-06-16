package com.lebhas.creativesaas.identity.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OnboardingRewardPolicyCommand(
        boolean active,
        boolean enableSignupFreeCredits,
        @NotNull @DecimalMin("0.0") BigDecimal signupFreeCredits,
        boolean enableEmailReward,
        @NotNull @DecimalMin("0.0") BigDecimal emailRewardCredits,
        boolean enableFacebookReward,
        @NotNull @DecimalMin("0.0") BigDecimal facebookRewardCredits,
        boolean enableInstagramReward,
        @NotNull @DecimalMin("0.0") BigDecimal instagramRewardCredits,
        boolean rewardOnlyOnce,
        boolean enableMobileOtpLogin,
        @Positive int otpExpiryMinutes,
        @Positive int otpResendCooldownSeconds,
        @Positive int maxOtpAttempts
) {
}
