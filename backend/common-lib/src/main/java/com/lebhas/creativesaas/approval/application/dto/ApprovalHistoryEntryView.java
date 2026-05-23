package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalAction;

import java.time.Instant;
import java.util.UUID;

public record ApprovalHistoryEntryView(
        UUID id,
        UUID approvalWorkflowId,
        UUID actionBy,
        ApprovalAction actionType,
        String comments,
        Instant createdAt
) {
}
