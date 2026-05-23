package com.lebhas.pricing;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceSubscriptionRepository extends TenantAwareRepository<WorkspaceSubscription> {

    Optional<WorkspaceSubscription> findFirstByWorkspaceIdAndDeletedFalse(UUID workspaceId);

    Optional<WorkspaceSubscription> findByWorkspaceIdAndStatusAndDeletedFalse(
            UUID workspaceId,
            WorkspaceSubscriptionStatus status
    );

    List<WorkspaceSubscription> findAllByPricingPlanIdAndDeletedFalse(UUID pricingPlanId);

    List<WorkspaceSubscription> findAllByStatusAndDeletedFalse(WorkspaceSubscriptionStatus status);
}
