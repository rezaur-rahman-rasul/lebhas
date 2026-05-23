package com.lebhas.creativesaas.usage.cache;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShareCounterCacheService {

    private final UsageBillingRedisKeys keys;
    private final UsageBillingRedisAccessSupport redis;
    private final UsageBillingRedisTtlStrategy ttl;

    public ShareCounterCacheService(UsageBillingRedisKeys keys, UsageBillingRedisAccessSupport redis, UsageBillingRedisTtlStrategy ttl) {
        this.keys = keys;
        this.redis = redis;
        this.ttl = ttl;
    }

    public Optional<CounterSnapshot> get(UUID workspaceId, LocalDate month) {
        return redis.read(keys.sharesCount(workspaceId, month), CounterSnapshot.class, "shares_count_get",
                UsageBillingRedisOperationContext.of(workspaceId, month));
    }

    public boolean put(UUID workspaceId, LocalDate month, long count) {
        return redis.write(keys.sharesCount(workspaceId, month), new CounterSnapshot(workspaceId, month, count, Instant.now()),
                ttl.counterTtl(), "shares_count_put", UsageBillingRedisOperationContext.of(workspaceId, month));
    }

    public boolean invalidate(UUID workspaceId, LocalDate month) {
        return redis.delete(keys.sharesCount(workspaceId, month), "shares_count_delete",
                UsageBillingRedisOperationContext.of(workspaceId, month));
    }

    public record CounterSnapshot(UUID workspaceId, LocalDate month, long count, Instant cachedAt) {
    }
}
