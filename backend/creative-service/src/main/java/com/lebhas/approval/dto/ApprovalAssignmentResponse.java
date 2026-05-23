package com.lebhas.approval.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalAssignmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Approval assignment response.")
public record ApprovalAssignmentResponse(
        @Schema(description = "Assignment id.")
        UUID id,
        @Schema(description = "Workspace id.")
        UUID workspaceId,
        @Schema(description = "Approval request id.")
        UUID approvalRequestId,
        @Schema(description = "Reviewer user id.")
        UUID reviewerId,
        @Schema(description = "Actor who performed the assignment.")
        UUID assignedBy,
        @Schema(description = "Assignment timestamp.", type = "string", format = "date-time")
        Instant assignedAt,
        @Schema(description = "Assignment status.")
        ApprovalAssignmentStatus assignmentStatus,
        @Schema(description = "Creation timestamp.", type = "string", format = "date-time")
        Instant createdAt,
        @Schema(description = "Last update timestamp.", type = "string", format = "date-time")
        Instant updatedAt
) {
}
