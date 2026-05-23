package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;

import java.time.Instant;
import java.util.UUID;

public record ApprovalRequestListCriteria(
        UUID workspaceId,
        ApprovalStatus status,
        UUID reviewerId,
        UUID submittedBy,
        Instant submittedFrom,
        Instant submittedTo,
        boolean pendingOnly,
        boolean approvedOnly
) {
}
