package com.lebhas.creativesaas.identity.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OnboardingRewardPolicyView(
        UUID id,
        boolean active,
        boolean enableSignupFreeCredits,
        BigDecimal signupFreeCredits,
        boolean enableEmailReward,
        BigDecimal emailRewardCredits,
        boolean enableFacebookReward,
        BigDecimal facebookRewardCredits,
        boolean enableInstagramReward,
        BigDecimal instagramRewardCredits,
        boolean enableProfileRewards,
        boolean rewardOnlyOnce,
        boolean enableMobileOtpLogin,
        int otpExpiryMinutes,
        int otpResendCooldownSeconds,
        int maxOtpAttempts,
        Instant updatedAt
) {
}
