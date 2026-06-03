package com.lebhas.creativesaas.campaignpackage.application.dto;

import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackageItemType;

import java.util.List;
import java.util.UUID;

public record CampaignPackageCommand(
        UUID workspaceId,
        UUID projectId,
        String name,
        String description,
        List<CampaignPackageItemCommand> items
) {
    public record CampaignPackageItemCommand(CampaignPackageItemType itemType, UUID itemId) {
    }
}
