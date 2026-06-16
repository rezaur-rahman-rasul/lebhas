package com.lebhas.creativesaas.identity.application.dto;

public record RegistrationProjectCampaignCommand(
        String registrationSessionToken,
        String projectCampaignName,
        String deviceId,
        String clientIp,
        String userAgent
) {
}
