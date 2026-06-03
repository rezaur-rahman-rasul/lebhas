package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProjectCampaignRequest(
        UUID productServiceId,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 140)
        String name,
        @Size(max = 2000)
        String description,
        @Size(max = 160)
        String campaignObjective,
        @Size(max = 120)
        String targetPlatform,
        @Size(max = 120)
        String campaignType
) {
    public CreateProjectCampaignRequest(
            String name,
            String description,
            String campaignObjective,
            String targetPlatform,
            String campaignType
    ) {
        this(null, name, description, campaignObjective, targetPlatform, campaignType);
    }
}
