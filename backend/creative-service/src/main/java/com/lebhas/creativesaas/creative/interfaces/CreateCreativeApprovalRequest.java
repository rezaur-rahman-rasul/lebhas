package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.approval.domain.CreativeApprovalPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Creates a draft approval workflow for a generated creative output.")
public record CreateCreativeApprovalRequest(
        @NotNull
        UUID creativeOutputId,
        CreativeApprovalPriority priority,
        Instant dueAt,
        @Size(max = 2000)
        String approvalNote
) {
}
