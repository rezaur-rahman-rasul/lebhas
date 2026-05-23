package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Shareable generated version summary included with resolved share links.")
public record GeneratedVersionSummaryResponse(
        UUID id,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID projectId,
        int versionNumber,
        String versionName,
        UUID storageFileId,
        GenerationStatus generationStatus,
        ApprovalStatus approvalStatus,
        boolean editableBeforeApproval,
        String generatedByProvider,
        String generatedByModel,
        UUID createdByUserId,
        GeneratedVersionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
