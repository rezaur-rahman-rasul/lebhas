package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.campaign.domain.ProjectCampaignStatus;
import com.lebhas.creativesaas.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectCampaignRequest(
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
        String campaignType,
        ProjectCampaignStatus status
) {
}
