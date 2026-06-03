package com.lebhas.creativesaas.generatedversion.application.dto;

import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionApprovalAction;

import java.time.Instant;
import java.util.UUID;

public record GeneratedVersionApprovalHistoryView(
        UUID id,
        UUID workspaceId,
        UUID generatedVersionId,
        GeneratedVersionApprovalAction action,
        UUID actionBy,
        String comment,
        Instant createdAt
) {
}
