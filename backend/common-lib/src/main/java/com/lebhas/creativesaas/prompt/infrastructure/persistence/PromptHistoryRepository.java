package com.lebhas.creativesaas.prompt.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.prompt.domain.PromptHistoryEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromptHistoryRepository extends TenantAwareRepository<PromptHistoryEntity>, JpaSpecificationExecutor<PromptHistoryEntity> {

    Optional<PromptHistoryEntity> findByIdAndWorkspaceIdAndDeletedFalse(UUID id, UUID workspaceId);

    List<PromptHistoryEntity> findAllByWorkspaceIdAndProjectCampaignIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID projectCampaignId
    );

    List<PromptHistoryEntity> findAllByWorkspaceIdAndCreativeRequestIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID creativeRequestId
    );
}
