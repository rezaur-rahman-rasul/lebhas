package com.lebhas.approval.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Approval request summary response.")
public record ApprovalRequestResponse(
        @Schema(description = "Approval request id.")
        UUID id,
        @Schema(description = "Workspace id.")
        UUID workspaceId,
        @Schema(description = "Generated version id.")
        UUID generatedVersionId,
        @Schema(description = "Project campaign id.")
        UUID projectCampaignId,
        @Schema(description = "Submitter user id.")
        UUID submittedBy,
        @Schema(description = "Assigned reviewer user id.")
        UUID reviewerId,
        @Schema(description = "Current approval status.")
        ApprovalStatus status,
        @Schema(description = "Timestamp when the request was submitted.", type = "string", format = "date-time")
        Instant submittedAt,
        @Schema(description = "Timestamp when the request was last reviewed.", type = "string", format = "date-time")
        Instant reviewedAt,
        @Schema(description = "Optional due date.", type = "string", format = "date-time")
        Instant dueAt,
        @Schema(description = "Latest approval comment.")
        String latestComment,
        @Schema(description = "Number of resubmissions.")
        int revisionCount,
        @Schema(description = "Creation timestamp.", type = "string", format = "date-time")
        Instant createdAt,
        @Schema(description = "Last update timestamp.", type = "string", format = "date-time")
        Instant updatedAt
) {
}
