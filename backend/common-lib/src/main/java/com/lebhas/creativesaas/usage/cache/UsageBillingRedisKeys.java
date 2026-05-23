package com.lebhas.creativesaas.usage.cache;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class UsageBillingRedisKeys {

    public String usageSummary(UUID workspaceId, LocalDate month) {
        return "usage:summary:" + require(workspaceId, "workspaceId") + ":" + normalizeMonth(month);
    }

    public String creditBalance(UUID workspaceId) {
        return "credits:balance:" + require(workspaceId, "workspaceId");
    }

    public String creditCompatibilityLock(UUID workspaceId) {
        return "credits:lock:" + require(workspaceId, "workspaceId");
    }

    public String monthlyUsage(UUID workspaceId, LocalDate month) {
        return "usage:monthly:" + require(workspaceId, "workspaceId") + ":" + normalizeMonth(month);
    }

    public String downloadsCount(UUID workspaceId, LocalDate month) {
        return "downloads:count:" + require(workspaceId, "workspaceId") + ":" + normalizeMonth(month);
    }

    public String sharesCount(UUID workspaceId, LocalDate month) {
        return "shares:count:" + require(workspaceId, "workspaceId") + ":" + normalizeMonth(month);
    }

    public String quotaValidation(UUID workspaceId) {
        return "quota:validation:" + require(workspaceId, "workspaceId");
    }

    public String aiLayerCost(UUID workspaceId, LocalDate month) {
        return "ai:layer:cost:" + require(workspaceId, "workspaceId") + ":" + normalizeMonth(month);
    }

    public String billingDashboard(UUID workspaceId) {
        return "billing:dashboard:" + require(workspaceId, "workspaceId");
    }

    public String creditLock(UUID workspaceId) {
        return "lock:credits:" + require(workspaceId, "workspaceId");
    }

    public String usageSummaryLock(UUID workspaceId, LocalDate month) {
        return "lock:usage-summary:" + require(workspaceId, "workspaceId") + ":" + normalizeMonth(month);
    }

    public String monthlySnapshotLock(UUID workspaceId, LocalDate month) {
        return "lock:monthly-snapshot:" + require(workspaceId, "workspaceId") + ":" + normalizeMonth(month);
    }

    public String quotaLock(UUID workspaceId) {
        return "lock:quota:" + require(workspaceId, "workspaceId");
    }

    private UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private LocalDate normalizeMonth(LocalDate month) {
        if (month == null) {
            throw new IllegalArgumentException("month must not be null");
        }
        return month.withDayOfMonth(1);
    }
}
