package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Creative request with its current job and generated version context.")
public record CreativeRequestResourceResponse(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        UUID requestedBy,
        String requestName,
        String sourcePrompt,
        String enhancedPrompt,
        BrandLanguagePreference languagePreference,
        String creativeObjective,
        String targetPlatform,
        String requestedFormat,
        int requestedVersions,
        List<UUID> selectedAssetIds,
        CreativeRequestStatus status,
        UUID creditReservationId,
        GeneratedVersionResponse latestVersion,
        List<GeneratedVersionResponse> versions,
        CreativeRequestJobResponse job,
        BigDecimal estimatedCreditCost,
        Instant createdAt,
        Instant updatedAt
) {
}
