package com.lebhas.creativesaas.billing.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

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
        Integer maxTeamMembers,
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal maxStorageGb,
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal monthlyCreditLimit,
        boolean allowApprovalWorkflow,
        boolean allowPublicShareLinks,
        boolean allowVideoGeneration,
        boolean allowAdvancedPromptIntelligence,
        boolean allowTeamCollaboration,
        boolean allowExportWithoutWatermark
) {
}
