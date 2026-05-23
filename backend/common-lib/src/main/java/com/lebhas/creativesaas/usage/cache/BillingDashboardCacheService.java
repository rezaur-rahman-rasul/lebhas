package com.lebhas.creativesaas.usage.cache;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BillingDashboardCacheService {

    private final UsageBillingRedisKeys keys;
    private final UsageBillingRedisAccessSupport redis;
    private final UsageBillingRedisTtlStrategy ttl;

    public BillingDashboardCacheService(UsageBillingRedisKeys keys, UsageBillingRedisAccessSupport redis, UsageBillingRedisTtlStrategy ttl) {
        this.keys = keys;
        this.redis = redis;
        this.ttl = ttl;
    }

    public Optional<BillingDashboardSnapshot> get(UUID workspaceId) {
        return redis.read(keys.billingDashboard(workspaceId), BillingDashboardSnapshot.class, "billing_dashboard_get",
                UsageBillingRedisOperationContext.of(workspaceId, null));
    }

    public boolean put(BillingDashboardSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        return redis.write(keys.billingDashboard(snapshot.workspaceId()), snapshot, ttl.billingDashboardTtl(),
                "billing_dashboard_put", UsageBillingRedisOperationContext.of(snapshot.workspaceId(), null));
    }

    public boolean invalidate(UUID workspaceId) {
        return redis.delete(keys.billingDashboard(workspaceId), "billing_dashboard_delete",
                UsageBillingRedisOperationContext.of(workspaceId, null));
    }

    public record BillingDashboardSnapshot(
            UUID workspaceId,
            Map<String, Object> summary,
            Map<String, Object> quotas,
            Map<String, Object> counters,
            Instant cachedAt
    ) {
    }
}
