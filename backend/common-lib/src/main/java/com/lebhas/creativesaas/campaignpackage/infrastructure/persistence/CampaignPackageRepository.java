package com.lebhas.creativesaas.campaignpackage.infrastructure.persistence;

import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackage;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignPackageRepository extends TenantAwareRepository<CampaignPackage> {
    List<CampaignPackage> findAllByWorkspaceIdAndProjectIdAndDeletedFalseOrderByCreatedAtDesc(UUID workspaceId, UUID projectId);
}
