package com.lebhas.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Approve an approval request.")
public record ApprovalApproveRequest(
        @Size(max = 2000, message = "feedback must be 2000 characters or fewer")
        @Schema(description = "Optional approval feedback.", maxLength = 2000)
        String feedback
) {
}
