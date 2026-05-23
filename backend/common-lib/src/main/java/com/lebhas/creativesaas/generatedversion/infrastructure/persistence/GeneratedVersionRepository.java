package com.lebhas.creativesaas.generatedversion.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;

import java.util.List;
import java.util.UUID;

public interface GeneratedVersionRepository extends TenantAwareRepository<GeneratedVersionEntity> {

    List<GeneratedVersionEntity> findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(UUID workspaceId);

    java.util.Optional<GeneratedVersionEntity> findByIdAndWorkspaceIdAndDeletedFalse(UUID id, UUID workspaceId);

    java.util.Optional<GeneratedVersionEntity> findByIdAndDeletedFalse(UUID id);

    List<GeneratedVersionEntity> findAllByWorkspaceIdAndCreativeRequestIdAndDeletedFalseOrderByVersionNumberDesc(
            UUID workspaceId,
            UUID creativeRequestId
    );

    long countByWorkspaceIdAndCreativeRequestIdAndDeletedFalse(UUID workspaceId, UUID creativeRequestId);

    List<GeneratedVersionEntity> findAllByWorkspaceIdAndProjectCampaignIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID projectCampaignId
    );

    java.util.Optional<GeneratedVersionEntity> findFirstByWorkspaceIdAndCreativeRequestIdAndDeletedFalseOrderByVersionNumberDesc(
            UUID workspaceId,
            UUID creativeRequestId
    );

    List<GeneratedVersionEntity> findAllByWorkspaceIdAndGenerationStatusAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            GenerationStatus generationStatus
    );

    List<GeneratedVersionEntity> findAllByWorkspaceIdAndGenerationStatusAndCreativeRequestIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            GenerationStatus generationStatus,
            UUID creativeRequestId
    );

    java.util.Optional<GeneratedVersionEntity> findByWorkspaceIdAndGeneratedAssetIdAndDeletedFalse(
            UUID workspaceId,
            UUID generatedAssetId
    );

    List<GeneratedVersionEntity> findAllByWorkspaceIdAndApprovalStatusAndDeletedFalseOrderByUpdatedAtDesc(
            UUID workspaceId,
            ApprovalStatus approvalStatus
    );

    List<GeneratedVersionEntity> findAllByWorkspaceIdAndLatestReviewerIdAndDeletedFalseOrderByUpdatedAtDesc(
            UUID workspaceId,
            UUID latestReviewerId
    );
}
