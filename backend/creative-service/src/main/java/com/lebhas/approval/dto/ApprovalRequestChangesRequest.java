package com.lebhas.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request changes on an approval request.")
public record ApprovalRequestChangesRequest(
        @NotBlank(message = "feedback is required")
        @Size(max = 2000, message = "feedback must be 2000 characters or fewer")
        @Schema(description = "Required change request details.", maxLength = 2000, requiredMode = Schema.RequiredMode.REQUIRED)
        String feedback
) {
}
