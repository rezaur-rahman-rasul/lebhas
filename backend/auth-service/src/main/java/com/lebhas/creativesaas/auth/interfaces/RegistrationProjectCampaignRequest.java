package com.lebhas.creativesaas.auth.interfaces;

import jakarta.validation.constraints.NotBlank;

public record RegistrationProjectCampaignRequest(
        @NotBlank String registrationSessionToken,
        @NotBlank String projectCampaignName,
        String deviceId
) {
}
