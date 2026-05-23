package com.lebhas.creativesaas.approval.infrastructure.persistence;

import com.lebhas.creativesaas.approval.domain.ApprovalReview;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.UUID;

@Deprecated(forRemoval = true)
public interface ApprovalReviewRepository extends TenantAwareRepository<ApprovalReview> {

    List<ApprovalReview> findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByReviewedAtAsc(
            UUID workspaceId,
            UUID approvalRequestId
    );

    List<ApprovalReview> findAllByWorkspaceIdAndReviewerIdAndDeletedFalseOrderByReviewedAtDesc(
            UUID workspaceId,
            UUID reviewerId
    );
}
