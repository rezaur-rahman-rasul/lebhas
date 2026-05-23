package com.lebhas.creativesaas.usage.cache;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsageMonthlyCounterService {

    private final UsageBillingRedisKeys keys;
    private final UsageBillingRedisAccessSupport redis;
    private final UsageBillingRedisTtlStrategy ttl;

    public UsageMonthlyCounterService(UsageBillingRedisKeys keys, UsageBillingRedisAccessSupport redis, UsageBillingRedisTtlStrategy ttl) {
        this.keys = keys;
        this.redis = redis;
        this.ttl = ttl;
    }

    public Optional<MonthlyUsageCounterSnapshot> get(UUID workspaceId, LocalDate month) {
        return redis.read(keys.monthlyUsage(workspaceId, month), MonthlyUsageCounterSnapshot.class, "usage_monthly_get",
                UsageBillingRedisOperationContext.of(workspaceId, month));
    }

    public boolean put(MonthlyUsageCounterSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        return redis.write(keys.monthlyUsage(snapshot.workspaceId(), snapshot.month()), snapshot, ttl.monthlyUsageTtl(),
                "usage_monthly_put", UsageBillingRedisOperationContext.of(snapshot.workspaceId(), snapshot.month()));
    }

    public boolean invalidate(UUID workspaceId, LocalDate month) {
        return redis.delete(keys.monthlyUsage(workspaceId, month), "usage_monthly_delete",
                UsageBillingRedisOperationContext.of(workspaceId, month));
    }

    public record MonthlyUsageCounterSnapshot(
            UUID workspaceId,
            LocalDate month,
            BigDecimal usedCredits,
            long generatedVersions,
            long creativeRequests,
            BigDecimal aiCostUsd,
            long downloads,
            long publicShares,
            Instant cachedAt
    ) {
    }
}
