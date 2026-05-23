package com.lebhas.creativesaas.usage.cache;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class UsageBillingRedisTtlStrategy {

    public Duration usageSummaryTtl() {
        return Duration.ofMinutes(30);
    }

    public Duration creditBalanceTtl() {
        return Duration.ofMinutes(10);
    }

    public Duration monthlyUsageTtl() {
        return Duration.ofHours(6);
    }

    public Duration counterTtl() {
        return Duration.ofDays(40);
    }

    public Duration quotaValidationTtl() {
        return Duration.ofMinutes(5);
    }

    public Duration aiLayerCostTtl() {
        return Duration.ofHours(2);
    }

    public Duration billingDashboardTtl() {
        return Duration.ofMinutes(15);
    }

    public Duration lockTtl() {
        return Duration.ofSeconds(45);
    }
}
