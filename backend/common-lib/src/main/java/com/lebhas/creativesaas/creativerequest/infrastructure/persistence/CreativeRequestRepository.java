package com.lebhas.creativesaas.creativerequest.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;

import java.util.List;
import java.util.UUID;

public interface CreativeRequestRepository extends TenantAwareRepository<CreativeRequestEntity> {

    List<CreativeRequestEntity> findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(UUID workspaceId);

    List<CreativeRequestEntity> findAllByWorkspaceIdAndBrandIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID brandId
    );

    List<CreativeRequestEntity> findAllByWorkspaceIdAndProductServiceIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID productServiceId
    );

    List<CreativeRequestEntity> findAllByWorkspaceIdAndProjectCampaignIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID projectCampaignId
    );

    List<CreativeRequestEntity> findAllByWorkspaceIdAndCreatedByUserIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID createdByUserId
    );

    List<CreativeRequestEntity> findAllByWorkspaceIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            CreativeRequestStatus status
    );
}
