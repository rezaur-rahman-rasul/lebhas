package com.lebhas.creativesaas.usage.cache;

import com.lebhas.creativesaas.usage.application.dto.CreditBalanceView;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CreditBalanceCacheService {

    private final UsageBillingRedisKeys keys;
    private final UsageBillingRedisAccessSupport redis;
    private final UsageBillingRedisTtlStrategy ttl;

    public CreditBalanceCacheService(UsageBillingRedisKeys keys, UsageBillingRedisAccessSupport redis, UsageBillingRedisTtlStrategy ttl) {
        this.keys = keys;
        this.redis = redis;
        this.ttl = ttl;
    }

    public Optional<CreditBalanceView> get(UUID workspaceId) {
        return redis.read(keys.creditBalance(workspaceId), CreditBalanceView.class, "credit_balance_get",
                UsageBillingRedisOperationContext.of(workspaceId, null));
    }

    public boolean put(CreditBalanceView balance) {
        if (balance == null) {
            return false;
        }
        return redis.write(keys.creditBalance(balance.workspaceId()), balance, ttl.creditBalanceTtl(), "credit_balance_put",
                UsageBillingRedisOperationContext.of(balance.workspaceId(), null));
    }

    public boolean invalidate(UUID workspaceId) {
        return redis.delete(keys.creditBalance(workspaceId), "credit_balance_delete",
                UsageBillingRedisOperationContext.of(workspaceId, null));
    }
}
