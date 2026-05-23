package com.lebhas.approval.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalAuditAction;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Approval audit trail entry.")
public record ApprovalAuditLogResponse(
        @Schema(description = "Audit log id.")
        UUID id,
        @Schema(description = "Workspace id.")
        UUID workspaceId,
        @Schema(description = "Approval request id.")
        UUID approvalRequestId,
        @Schema(description = "Generated version id.")
        UUID generatedVersionId,
        @Schema(description = "Actor user id.")
        UUID actorUserId,
        @Schema(description = "Audit action.")
        ApprovalAuditAction action,
        @Schema(description = "Previous approval status.")
        ApprovalStatus previousStatus,
        @Schema(description = "New approval status.")
        ApprovalStatus newStatus,
        @Schema(description = "Audit details.")
        String details,
        @Schema(description = "Audit timestamp.", type = "string", format = "date-time")
        Instant createdAt
) {
}
