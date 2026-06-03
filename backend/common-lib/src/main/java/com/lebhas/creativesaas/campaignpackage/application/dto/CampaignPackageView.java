package com.lebhas.creativesaas.campaignpackage.application.dto;

import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackageItemType;
import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackageStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CampaignPackageView(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        String name,
        String description,
        CampaignPackageStatus status,
        String r2ObjectKey,
        String exportUrl,
        Instant exportUrlExpiresAt,
        List<ItemView> items
) {
    public record ItemView(UUID id, CampaignPackageItemType itemType, UUID itemId) {
    }
}
