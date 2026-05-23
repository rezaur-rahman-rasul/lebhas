package com.lebhas.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Reject an approval request.")
public record ApprovalRejectRequest(
        @NotBlank(message = "feedback is required")
        @Size(max = 2000, message = "feedback must be 2000 characters or fewer")
        @Schema(description = "Reason for rejection.", maxLength = 2000, requiredMode = Schema.RequiredMode.REQUIRED)
        String feedback
) {
}
