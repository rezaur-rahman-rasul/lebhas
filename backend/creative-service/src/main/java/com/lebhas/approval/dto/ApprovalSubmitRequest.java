package com.lebhas.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Submit a generated version into the approval workflow.")
public record ApprovalSubmitRequest(
        @Schema(description = "Optional due date for the approval request.", type = "string", format = "date-time")
        Instant dueAt,
        @Size(max = 2000, message = "submissionComment must be 2000 characters or fewer")
        @Schema(description = "Optional submission note for reviewers.", maxLength = 2000)
        String submissionComment
) {
}
