package com.lebhas.creativesaas.creativerequest.cache;

import com.lebhas.creativesaas.creativerequest.cache.dto.GenerationQuotaCacheEntry;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.prompt.cache.PromptRedisAccessSupport;
import com.lebhas.creativesaas.prompt.cache.PromptRedisKeys;
import com.lebhas.creativesaas.prompt.cache.PromptRedisOperationContext;
import com.lebhas.creativesaas.prompt.cache.PromptRedisTtlStrategy;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class GenerationQuotaCacheService {

    private final PromptRedisAccessSupport redisAccessSupport;
    private final PromptRedisTtlStrategy ttlStrategy;

    public GenerationQuotaCacheService(
            PromptRedisAccessSupport redisAccessSupport,
            PromptRedisTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<GenerationQuotaCacheEntry> get(UUID workspaceId) {
        return redisAccessSupport.read(
                PromptRedisKeys.generationQuota(workspaceId),
                GenerationQuotaCacheEntry.class,
                "generation_quota_read",
                PromptRedisOperationContext.generationQuota(workspaceId, null));
    }

    public GenerationQuotaCacheEntry getOrLoad(UUID workspaceId, Supplier<GenerationQuotaCacheEntry> loader) {
        return get(workspaceId).orElseGet(() -> {
            GenerationQuotaCacheEntry loaded = loader.get();
            if (loaded != null) {
                store(loaded);
            }
            return loaded;
        });
    }

    public void store(WorkspacePlanContextView planContext) {
        store(GenerationQuotaCacheEntry.from(planContext));
    }

    public void store(GenerationQuotaCacheEntry entry) {
        if (entry == null || entry.workspaceId() == null) {
            return;
        }
        redisAccessSupport.write(
                PromptRedisKeys.generationQuota(entry.workspaceId()),
                entry,
                ttlStrategy.generationQuotaTtl(),
                "generation_quota_write",
                PromptRedisOperationContext.generationQuota(entry.workspaceId(), entry.pricingPlanId()));
    }

    public void invalidate(UUID workspaceId) {
        redisAccessSupport.delete(
                PromptRedisKeys.generationQuota(workspaceId),
                "generation_quota_delete",
                PromptRedisOperationContext.generationQuota(workspaceId, null));
    }
}
