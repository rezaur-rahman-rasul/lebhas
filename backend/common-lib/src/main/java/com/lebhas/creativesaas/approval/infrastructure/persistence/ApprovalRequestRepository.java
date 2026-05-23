package com.lebhas.creativesaas.approval.infrastructure.persistence;

import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Deprecated(forRemoval = true)
public interface ApprovalRequestRepository extends TenantAwareRepository<ApprovalRequest>, JpaSpecificationExecutor<ApprovalRequest> {

    Optional<ApprovalRequest> findByIdAndWorkspaceIdAndDeletedFalse(UUID id, UUID workspaceId);

    List<ApprovalRequest> findAllByWorkspaceIdAndProjectCampaignIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID projectCampaignId
    );

    List<ApprovalRequest> findAllByWorkspaceIdAndGeneratedVersionIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID generatedVersionId
    );

    List<ApprovalRequest> findAllByWorkspaceIdAndAssignedReviewerIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID assignedReviewerId
    );

    List<ApprovalRequest> findAllByWorkspaceIdAndCurrentStatusAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            ApprovalStatus currentStatus
    );

    List<ApprovalRequest> findAllByWorkspaceIdAndCurrentStatusInAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            Collection<ApprovalStatus> currentStatuses
    );

    Optional<ApprovalRequest> findFirstByWorkspaceIdAndGeneratedVersionIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID generatedVersionId
    );

    boolean existsByWorkspaceIdAndGeneratedVersionIdAndCurrentStatusInAndDeletedFalse(
            UUID workspaceId,
            UUID generatedVersionId,
            Collection<ApprovalStatus> currentStatuses
    );
}
