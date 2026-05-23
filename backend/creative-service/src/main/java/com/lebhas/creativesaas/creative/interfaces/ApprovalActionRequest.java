package com.lebhas.creativesaas.creative.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Approval action payload.")
public record ApprovalActionRequest(
        @Size(max = 2000)
        @Schema(description = "Optional reviewer comments", maxLength = 2000)
        String comments
) {
}
