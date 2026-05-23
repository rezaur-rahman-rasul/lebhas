package com.lebhas.creativesaas.usage.cache;

import com.lebhas.creativesaas.usage.application.dto.WorkspaceUsageSummaryView;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkspaceUsageSummaryCacheService {

    private final UsageBillingRedisKeys keys;
    private final UsageBillingRedisAccessSupport redis;
    private final UsageBillingRedisTtlStrategy ttl;

    public WorkspaceUsageSummaryCacheService(
            UsageBillingRedisKeys keys,
            UsageBillingRedisAccessSupport redis,
            UsageBillingRedisTtlStrategy ttl
    ) {
        this.keys = keys;
        this.redis = redis;
        this.ttl = ttl;
    }

    public Optional<WorkspaceUsageSummaryView> get(UUID workspaceId, LocalDate month) {
        return redis.read(keys.usageSummary(workspaceId, month), WorkspaceUsageSummaryView.class, "usage_summary_get",
                UsageBillingRedisOperationContext.of(workspaceId, month));
    }

    public boolean put(WorkspaceUsageSummaryView summary) {
        if (summary == null) {
            return false;
        }
        return redis.write(keys.usageSummary(summary.workspaceId(), summary.usageMonth()), summary, ttl.usageSummaryTtl(),
                "usage_summary_put", UsageBillingRedisOperationContext.of(summary.workspaceId(), summary.usageMonth()));
    }

    public boolean invalidate(UUID workspaceId, LocalDate month) {
        return redis.delete(keys.usageSummary(workspaceId, month), "usage_summary_delete",
                UsageBillingRedisOperationContext.of(workspaceId, month));
    }
}
