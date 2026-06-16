package com.lebhas.creativesaas.identity.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RegistrationStepView(
        String registrationSessionToken,
        String nextStep,
        String mobileNumberMasked,
        String emailMasked,
        boolean isNewUser,
        int resendAfterSeconds,
        int otpLength,
        BigDecimal creditsGranted,
        UUID workspaceId,
        UUID brandId,
        UUID productServiceId,
        UUID projectCampaignId
) {
}
