package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalAuditAction;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;

import java.time.Instant;
import java.util.UUID;

public record ApprovalAuditLogView(
        UUID id,
        UUID workspaceId,
        UUID approvalRequestId,
        UUID generatedVersionId,
        UUID actorId,
        ApprovalAuditAction action,
        ApprovalStatus previousStatus,
        ApprovalStatus newStatus,
        String details,
        Instant createdAt
) {
}
