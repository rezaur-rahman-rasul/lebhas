package com.lebhas.creativesaas.billing.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Set;

@Schema(description = "Request payload to create or update a pricing plan feature policy.")
public record UpdatePlanFeaturePolicyRequest(
        @PositiveOrZero
        Integer maxGeneratedVersionsPerRequest,
        @PositiveOrZero
        Integer maxBrands,
        @PositiveOrZero
        Integer maxProductServices,
        @PositiveOrZero
        Integer maxProjects,
        @PositiveOrZero
        Integer maxAssets,
        @PositiveOrZero
        Integer maxCreativeRequests,
        @PositiveOrZero
        Integer maxTeamMembers,
        @PositiveOrZero
        Integer maxGeneratedVersionsPerCreativeRequest,
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal maxStorageGb,
        @PositiveOrZero
        Long maxStorageBytes,
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal monthlyCreditLimit,
        boolean promptEnhancementEnabled,
        boolean creativeGenerationEnabled,
        boolean allowApprovalWorkflow,
        boolean downloadEnabled,
        boolean shareEnabled,
        boolean allowPublicShareLinks,
        boolean assetUploadEnabled,
        boolean premiumQualityEnabled,
        boolean allowVideoGeneration,
        boolean voiceoverGenerationEnabled,
        boolean allowAdvancedPromptIntelligence,
        boolean allowTeamCollaboration,
        boolean allowExportWithoutWatermark,
        Set<String> enabledCreativeToolCodes
) {
}
