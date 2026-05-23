package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.usage.application.dto.UsageBillingLogView;
import com.lebhas.creativesaas.usage.domain.UsageBillingLog;
import org.springframework.stereotype.Component;

@Component
public class UsageBillingMapper {

    public UsageBillingLogView toView(UsageBillingLog log) {
        return new UsageBillingLogView(
                log.getId(),
                log.getWorkspaceId(),
                log.getUsageType(),
                log.getReferenceType(),
                log.getReferenceId(),
                log.getCreditsCharged(),
                log.getEstimatedCostUsd(),
                log.getPricingPlanId(),
                log.getPlanFeaturePolicyId(),
                log.getCreatedAt());
    }
}
