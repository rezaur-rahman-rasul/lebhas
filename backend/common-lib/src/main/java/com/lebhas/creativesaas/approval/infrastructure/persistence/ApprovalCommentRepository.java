package com.lebhas.creativesaas.approval.infrastructure.persistence;

import com.lebhas.creativesaas.approval.domain.ApprovalComment;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.UUID;

@Deprecated(forRemoval = true)
public interface ApprovalCommentRepository extends TenantAwareRepository<ApprovalComment> {

    List<ApprovalComment> findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByCreatedAtAsc(
            UUID workspaceId,
            UUID approvalRequestId
    );

    List<ApprovalComment> findAllByWorkspaceIdAndGeneratedVersionIdAndDeletedFalseOrderByCreatedAtAsc(
            UUID workspaceId,
            UUID generatedVersionId
    );
}
