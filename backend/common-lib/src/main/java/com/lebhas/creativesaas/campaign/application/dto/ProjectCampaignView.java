package com.lebhas.creativesaas.campaign.application.dto;

import com.lebhas.creativesaas.campaign.domain.ProjectCampaignStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectCampaignView(
        UUID id,
        UUID workspaceId,
        UUID brandId,
        UUID productServiceId,
        UUID createdByUserId,
        String name,
        String description,
        String campaignObjective,
        String targetPlatform,
        String campaignType,
        ProjectCampaignStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
