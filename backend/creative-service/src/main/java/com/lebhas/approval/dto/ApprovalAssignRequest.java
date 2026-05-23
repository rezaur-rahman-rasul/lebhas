package com.lebhas.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Assign or reassign an approval request to a reviewer.")
public record ApprovalAssignRequest(
        @NotNull(message = "reviewerId is required")
        @Schema(description = "Workspace user id of the reviewer.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID reviewerId
) {
}
