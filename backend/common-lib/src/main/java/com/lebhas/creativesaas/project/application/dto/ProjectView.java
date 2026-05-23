package com.lebhas.creativesaas.project.application.dto;

import com.lebhas.creativesaas.project.domain.ProjectStatus;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.time.Instant;
import java.util.UUID;

public record ProjectView(
        UUID id,
        UUID workspaceId,
        UUID brandId,
        String name,
        String description,
        CampaignObjective campaignObjective,
        PromptPlatform targetPlatform,
        ProjectStatus status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
