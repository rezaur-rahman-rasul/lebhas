package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalDecision;
import com.lebhas.creativesaas.approval.domain.ApprovalReviewType;

import java.time.Instant;
import java.util.UUID;

public record ApprovalReviewView(
        UUID id,
        UUID workspaceId,
        UUID approvalRequestId,
        UUID reviewerId,
        ApprovalDecision decision,
        String feedback,
        ApprovalReviewType reviewType,
        Instant reviewedAt,
        Instant createdAt
) {
}
