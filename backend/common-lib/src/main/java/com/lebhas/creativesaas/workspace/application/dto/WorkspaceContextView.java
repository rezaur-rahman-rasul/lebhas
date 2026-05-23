package com.lebhas.creativesaas.workspace.application.dto;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record WorkspaceContextView(
        UUID workspaceId,
        String workspaceName,
        Role role,
        Set<Permission> permissions,
        boolean canDownloadCreative,
        boolean canEditCreative,
        long activeUserCount,
        long activeSessionCount,
        long permissionVersion,
        boolean supportModeActive,
        Instant supportModeStartedAt,
        Instant supportModeExpiresAt,
        PricingPlanView activePricingPlan,
        WorkspaceSubscriptionView activeSubscription,
        PlanFeaturePolicyView planFeaturePolicy,
        Integer generatedVersionLimit,
        BigDecimal storageLimitGb,
        boolean approvalWorkflowAvailable,
        boolean publicShareAvailability,
        Integer teamMemberLimit,
        BigDecimal creditLimit,
        Instant cachedAt
) {
}
