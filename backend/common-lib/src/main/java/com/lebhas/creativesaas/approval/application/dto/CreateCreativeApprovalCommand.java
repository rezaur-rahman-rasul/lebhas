package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.CreativeApprovalPriority;

import java.time.Instant;
import java.util.UUID;

public record CreateCreativeApprovalCommand(
        UUID workspaceId,
        UUID creativeOutputId,
        CreativeApprovalPriority priority,
        Instant dueAt,
        String approvalNote
) {
}
