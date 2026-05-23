package com.lebhas.creativesaas.usage.cache;

import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsageBillingLockService {

    private final UsageBillingRedisKeys keys;
    private final UsageBillingRedisAccessSupport redis;
    private final UsageBillingRedisTtlStrategy ttl;

    public UsageBillingLockService(UsageBillingRedisKeys keys, UsageBillingRedisAccessSupport redis, UsageBillingRedisTtlStrategy ttl) {
        this.keys = keys;
        this.redis = redis;
        this.ttl = ttl;
    }

    public Optional<RedisLockService.RedisLockToken> acquireCreditLock(UUID workspaceId) {
        return redis.acquireLock(keys.creditLock(workspaceId), ttl.lockTtl(), "credit_lock_acquire",
                UsageBillingRedisOperationContext.of(workspaceId, null));
    }

    public Optional<RedisLockService.RedisLockToken> acquireUsageSummaryLock(UUID workspaceId, LocalDate month) {
        return redis.acquireLock(keys.usageSummaryLock(workspaceId, month), ttl.lockTtl(), "usage_summary_lock_acquire",
                UsageBillingRedisOperationContext.of(workspaceId, month));
    }

    public Optional<RedisLockService.RedisLockToken> acquireMonthlySnapshotLock(UUID workspaceId, LocalDate month) {
        return redis.acquireLock(keys.monthlySnapshotLock(workspaceId, month), ttl.lockTtl(), "monthly_snapshot_lock_acquire",
                UsageBillingRedisOperationContext.of(workspaceId, month));
    }

    public Optional<RedisLockService.RedisLockToken> acquireQuotaLock(UUID workspaceId) {
        return redis.acquireLock(keys.quotaLock(workspaceId), ttl.lockTtl(), "quota_lock_acquire",
                UsageBillingRedisOperationContext.of(workspaceId, null));
    }

    public boolean release(RedisLockService.RedisLockToken token, UUID workspaceId, LocalDate month) {
        return redis.releaseLock(token, "usage_billing_lock_release",
                UsageBillingRedisOperationContext.of(workspaceId, month));
    }
}
