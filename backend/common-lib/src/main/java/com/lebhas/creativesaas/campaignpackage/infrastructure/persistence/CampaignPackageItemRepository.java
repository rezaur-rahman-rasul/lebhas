package com.lebhas.creativesaas.campaignpackage.infrastructure.persistence;

import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackageItem;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignPackageItemRepository extends TenantAwareRepository<CampaignPackageItem> {
    List<CampaignPackageItem> findAllByWorkspaceIdAndCampaignPackageIdAndDeletedFalse(UUID workspaceId, UUID campaignPackageId);
}
