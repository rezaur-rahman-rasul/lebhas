package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.CreativeApprovalAction;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalStatus;

import java.time.Instant;
import java.util.UUID;

public record CreativeApprovalHistoryView(
        UUID id,
        UUID workspaceId,
        UUID approvalId,
        UUID creativeOutputId,
        CreativeApprovalAction action,
        CreativeApprovalStatus previousStatus,
        CreativeApprovalStatus newStatus,
        UUID actorId,
        String note,
        Instant createdAt
) {
}
