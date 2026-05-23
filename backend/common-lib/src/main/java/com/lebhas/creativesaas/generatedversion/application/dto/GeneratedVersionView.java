package com.lebhas.creativesaas.generatedversion.application.dto;

import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;

import java.time.Instant;
import java.util.UUID;

public record GeneratedVersionView(
        UUID id,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID projectCampaignId,
        int versionNumber,
        String versionName,
        UUID storageFileId,
        UUID assetId,
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
