package com.lebhas.creativesaas.pricing.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlanFeaturePolicyView(
        UUID id,
        UUID pricingPlanId,
        Integer maxGeneratedVersionsPerRequest,
        Integer maxBrands,
        Integer maxProductServices,
        Integer maxProjects,
        Integer maxTeamMembers,
        BigDecimal maxStorageGb,
        BigDecimal monthlyCreditLimit,
        boolean allowApprovalWorkflow,
        boolean allowPublicShareLinks,
        boolean allowVideoGeneration,
        boolean allowAdvancedPromptIntelligence,
        boolean allowTeamCollaboration,
        boolean allowExportWithoutWatermark,
        Instant createdAt,
        Instant updatedAt
) {
}
