package com.lebhas.creativesaas.billing.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Workspace subscription and active plan context response.")
public record WorkspacePlanContextResponse(
        UUID workspaceId,
        PricingPlanResponse activePricingPlan,
        WorkspaceSubscriptionResponse activeSubscription,
        PlanFeaturePolicyResponse planFeaturePolicy,
        Integer generatedVersionLimit,
        BigDecimal storageLimitGb,
        boolean approvalWorkflowAvailable,
        boolean publicShareAvailability,
        Integer teamMemberLimit,
        BigDecimal creditLimit,
        boolean defaultPlanApplied
) {
}
