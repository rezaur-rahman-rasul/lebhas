package com.lebhas.creativesaas.approval.infrastructure.persistence;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, UUID> {

    Optional<ApprovalWorkflow> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<ApprovalWorkflow> findByWorkspaceIdAndGeneratedVersionId(UUID workspaceId, UUID generatedVersionId);

    boolean existsByWorkspaceIdAndGeneratedVersionId(UUID workspaceId, UUID generatedVersionId);

    List<ApprovalWorkflow> findAllByWorkspaceIdAndCreativeRequestIdOrderByCreatedAtDesc(UUID workspaceId, UUID creativeRequestId);

    List<ApprovalWorkflow> findAllByWorkspaceIdAndCurrentReviewerIdOrderByCreatedAtDesc(UUID workspaceId, UUID currentReviewerId);

    List<ApprovalWorkflow> findAllByWorkspaceIdAndCurrentStatusInOrderByCreatedAtDesc(
            UUID workspaceId,
            Collection<ApprovalStatus> currentStatuses
    );
}
