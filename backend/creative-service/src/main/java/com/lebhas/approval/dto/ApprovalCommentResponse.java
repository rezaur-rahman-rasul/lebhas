package com.lebhas.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Approval comment response.")
public record ApprovalCommentResponse(
        @Schema(description = "Comment id.")
        UUID id,
        @Schema(description = "Workspace id.")
        UUID workspaceId,
        @Schema(description = "Approval request id.")
        UUID approvalRequestId,
        @Schema(description = "Generated version id.")
        UUID generatedVersionId,
        @Schema(description = "Comment author user id.")
        UUID authorUserId,
        @Schema(description = "Comment text.")
        String commentText,
        @Schema(description = "Whether the comment is internal-only.")
        boolean internalOnly,
        @Schema(description = "Creation timestamp.", type = "string", format = "date-time")
        Instant createdAt,
        @Schema(description = "Last update timestamp.", type = "string", format = "date-time")
        Instant updatedAt
) {
}
