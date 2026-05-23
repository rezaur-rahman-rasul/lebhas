package com.lebhas.creativesaas.approval.infrastructure.persistence;

import com.lebhas.creativesaas.approval.domain.ApprovalAssignment;
import com.lebhas.creativesaas.approval.domain.ApprovalAssignmentStatus;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Deprecated(forRemoval = true)
public interface ApprovalAssignmentRepository extends TenantAwareRepository<ApprovalAssignment> {

    List<ApprovalAssignment> findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByAssignedAtDesc(
            UUID workspaceId,
            UUID approvalRequestId
    );

    List<ApprovalAssignment> findAllByWorkspaceIdAndAssignedToAndDeletedFalseOrderByAssignedAtDesc(
            UUID workspaceId,
            UUID assignedTo
    );

    Optional<ApprovalAssignment> findFirstByWorkspaceIdAndApprovalRequestIdAndAssignmentStatusAndDeletedFalseOrderByAssignedAtDesc(
            UUID workspaceId,
            UUID approvalRequestId,
            ApprovalAssignmentStatus assignmentStatus
    );

    Optional<ApprovalAssignment> findFirstByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByAssignedAtDesc(
            UUID workspaceId,
            UUID approvalRequestId
    );
}
