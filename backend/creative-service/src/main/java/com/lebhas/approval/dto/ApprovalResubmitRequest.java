package com.lebhas.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Resubmit an approval request after updates.")
public record ApprovalResubmitRequest(
        @Size(max = 2000, message = "resubmissionComment must be 2000 characters or fewer")
        @Schema(description = "Optional note describing what changed.", maxLength = 2000)
        String resubmissionComment
) {
}
