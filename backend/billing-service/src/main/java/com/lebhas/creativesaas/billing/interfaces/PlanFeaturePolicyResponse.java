package com.lebhas.creativesaas.billing.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Pricing plan feature policy response.")
public record PlanFeaturePolicyResponse(
        UUID id,
        UUID pricingPlanId,
        Integer maxGeneratedVersionsPerRequest,
        Integer maxBrands,
        Integer maxProductServices,
        Integer maxProjects,
        Integer maxAssets,
        Integer maxCreativeRequests,
        Integer maxTeamMembers,
        Integer maxGeneratedVersionsPerCreativeRequest,
        BigDecimal maxStorageGb,
        Long maxStorageBytes,
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
        Set<String> enabledCreativeToolCodes,
        Instant createdAt,
        Instant updatedAt
) {
}
