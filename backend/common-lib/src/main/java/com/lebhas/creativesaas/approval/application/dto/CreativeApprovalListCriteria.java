package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.CreativeApprovalPriority;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalStatus;

import java.util.UUID;

public record CreativeApprovalListCriteria(
        UUID workspaceId,
        UUID creativeOutputId,
        UUID generationRequestId,
        UUID submittedBy,
        UUID reviewedBy,
        CreativeApprovalStatus status,
        CreativeApprovalPriority priority,
        int page,
        int size
) {
}
