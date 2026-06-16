package com.lebhas.creativesaas.campaign.infrastructure.persistence;

import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectCampaignRepository extends TenantAwareRepository<ProjectCampaignEntity> {

    List<ProjectCampaignEntity> findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(UUID workspaceId);

    Optional<ProjectCampaignEntity> findFirstByWorkspaceIdAndBrandIdAndProductServiceIdAndNameIgnoreCaseAndDeletedFalse(
            UUID workspaceId,
            UUID brandId,
            UUID productServiceId,
            String name
    );
}
