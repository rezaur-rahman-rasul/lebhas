package com.lebhas.creativesaas.pricing.application.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record UpdatePlanFeaturePolicyCommand(
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
        Set<String> enabledCreativeToolCodes
) {
}
