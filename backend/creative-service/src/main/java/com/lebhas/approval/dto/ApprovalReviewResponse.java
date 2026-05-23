package com.lebhas.approval.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalDecision;
import com.lebhas.creativesaas.approval.domain.ApprovalReviewType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Approval review decision response.")
public record ApprovalReviewResponse(
        @Schema(description = "Review id.")
        UUID id,
        @Schema(description = "Workspace id.")
        UUID workspaceId,
        @Schema(description = "Approval request id.")
        UUID approvalRequestId,
        @Schema(description = "Reviewer user id.")
        UUID reviewerId,
        @Schema(description = "Approval decision.")
        ApprovalDecision decision,
        @Schema(description = "Reviewer feedback.")
        String feedback,
        @Schema(description = "Initial or resubmission review.")
        ApprovalReviewType reviewType,
        @Schema(description = "Review timestamp.", type = "string", format = "date-time")
        Instant reviewedAt,
        @Schema(description = "Creation timestamp.", type = "string", format = "date-time")
        Instant createdAt
) {
}
