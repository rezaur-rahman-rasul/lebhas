package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalDecision;
import com.lebhas.creativesaas.approval.domain.ApprovalReviewType;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;

import java.time.Instant;
import java.util.UUID;

public record ApprovalReviewView(
        UUID id,
        UUID workspaceId,
        UUID approvalRequestId,
        UUID reviewerId,
        SafeProfileDisplayView reviewerDisplay,
        ApprovalDecision decision,
        String feedback,
        ApprovalReviewType reviewType,
        Instant reviewedAt,
        Instant createdAt
) {
}
