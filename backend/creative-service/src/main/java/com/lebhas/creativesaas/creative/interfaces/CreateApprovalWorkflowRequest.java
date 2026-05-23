package com.lebhas.creativesaas.creative.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Creates an approval workflow for a generated version.")
public record CreateApprovalWorkflowRequest(
        @NotNull
        @Schema(description = "Creative request identifier", format = "uuid")
        UUID creativeRequestId,
        @NotNull
        @Schema(description = "Generated version identifier", format = "uuid")
        UUID generatedVersionId,
        @Schema(description = "Current reviewer identifier", format = "uuid")
        UUID currentReviewerId
) {
}
