package com.lebhas.creativesaas.pricing.cache;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;

@Service
public class PricingCacheInvalidationService {

    private final PricingPlanCacheService pricingPlanCacheService;
    private final PlanFeaturePolicyCacheService planFeaturePolicyCacheService;
    private final WorkspaceSubscriptionCacheService workspaceSubscriptionCacheService;

    public PricingCacheInvalidationService(
            PricingPlanCacheService pricingPlanCacheService,
            PlanFeaturePolicyCacheService planFeaturePolicyCacheService,
            WorkspaceSubscriptionCacheService workspaceSubscriptionCacheService
    ) {
        this.pricingPlanCacheService = pricingPlanCacheService;
        this.planFeaturePolicyCacheService = planFeaturePolicyCacheService;
        this.workspaceSubscriptionCacheService = workspaceSubscriptionCacheService;
    }

    public void invalidatePricingPlanUpdated(UUID planId) {
        pricingPlanCacheService.invalidate(planId);
        pricingPlanCacheService.invalidateActivePlans();
    }

    public void invalidateFeaturePolicyUpdated(UUID planId) {
        planFeaturePolicyCacheService.invalidate(planId);
    }

    public void invalidateWorkspaceSubscriptionChanged(UUID workspaceId) {
        workspaceSubscriptionCacheService.invalidate(workspaceId);
    }

    public void invalidateWorkspaceSubscriptions(Collection<UUID> workspaceIds) {
        if (workspaceIds == null || workspaceIds.isEmpty()) {
            return;
        }
        workspaceIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .forEach(workspaceSubscriptionCacheService::invalidate);
    }

    public void invalidatePricingPlanDisabled(UUID planId) {
        pricingPlanCacheService.invalidate(planId);
        pricingPlanCacheService.invalidateActivePlans();
        planFeaturePolicyCacheService.invalidate(planId);
    }

    public void invalidatePlanRelatedCaches(UUID planId) {
        pricingPlanCacheService.invalidate(planId);
        pricingPlanCacheService.invalidateActivePlans();
        planFeaturePolicyCacheService.invalidate(planId);
    }
}
