package com.lebhas.creativesaas.usage.cache;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class QuotaValidationCacheService {

    private final UsageBillingRedisKeys keys;
    private final UsageBillingRedisAccessSupport redis;
    private final UsageBillingRedisTtlStrategy ttl;

    public QuotaValidationCacheService(UsageBillingRedisKeys keys, UsageBillingRedisAccessSupport redis, UsageBillingRedisTtlStrategy ttl) {
        this.keys = keys;
        this.redis = redis;
        this.ttl = ttl;
    }

    public Optional<QuotaValidationSnapshot> get(UUID workspaceId) {
        return redis.read(keys.quotaValidation(workspaceId), QuotaValidationSnapshot.class, "quota_validation_get",
                UsageBillingRedisOperationContext.of(workspaceId, null));
    }

    public boolean put(QuotaValidationSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        return redis.write(keys.quotaValidation(snapshot.workspaceId()), snapshot, ttl.quotaValidationTtl(), "quota_validation_put",
                UsageBillingRedisOperationContext.of(snapshot.workspaceId(), null));
    }

    public boolean invalidate(UUID workspaceId) {
        return redis.delete(keys.quotaValidation(workspaceId), "quota_validation_delete",
                UsageBillingRedisOperationContext.of(workspaceId, null));
    }

    public record QuotaValidationSnapshot(
            UUID workspaceId,
            boolean valid,
            Map<String, Object> limits,
            Map<String, Object> usage,
            Instant cachedAt
    ) {
    }
}
