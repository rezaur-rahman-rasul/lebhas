package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackageItemType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record CampaignPackageApiRequest(
        @NotBlank String name,
        String description,
        @Valid List<ItemRequest> items
) {
    public record ItemRequest(CampaignPackageItemType itemType, UUID itemId) {
    }
}
