package com.lebhas.creativesaas.creativerequest.cache;

import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestView;
import com.lebhas.creativesaas.creativerequest.cache.dto.CreativeRequestCacheEntry;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.prompt.cache.PromptRedisAccessSupport;
import com.lebhas.creativesaas.prompt.cache.PromptRedisKeys;
import com.lebhas.creativesaas.prompt.cache.PromptRedisOperationContext;
import com.lebhas.creativesaas.prompt.cache.PromptRedisTtlStrategy;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class CreativeRequestCacheService {

    private final CreativeRequestRepository creativeRequestRepository;
    private final PromptRedisAccessSupport redisAccessSupport;
    private final PromptRedisTtlStrategy ttlStrategy;

    public CreativeRequestCacheService(
            CreativeRequestRepository creativeRequestRepository,
            PromptRedisAccessSupport redisAccessSupport,
            PromptRedisTtlStrategy ttlStrategy
    ) {
        this.creativeRequestRepository = creativeRequestRepository;
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<CreativeRequestCacheEntry> get(UUID workspaceId, UUID requestId) {
        return redisAccessSupport.read(
                PromptRedisKeys.creativeRequest(requestId),
                CreativeRequestCacheEntry.class,
                "creative_request_read",
                PromptRedisOperationContext.creativeRequest(workspaceId, requestId));
    }

    public CreativeRequestCacheEntry getOrLoad(UUID workspaceId, UUID requestId) {
        return getOrLoad(workspaceId, requestId, () -> creativeRequestRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(requestId, workspaceId)
                .map(CreativeRequestCacheEntry::from)
                .orElse(null));
    }

    public CreativeRequestCacheEntry getOrLoad(
            UUID workspaceId,
            UUID requestId,
            Supplier<CreativeRequestCacheEntry> loader
    ) {
        return get(workspaceId, requestId).orElseGet(() -> {
            CreativeRequestCacheEntry loaded = loader.get();
            if (loaded != null) {
                store(loaded);
            }
            return loaded;
        });
    }

    public void store(CreativeRequestEntity entity) {
        store(CreativeRequestCacheEntry.from(entity));
    }

    public void store(CreativeRequestView view) {
        store(CreativeRequestCacheEntry.from(view));
    }

    public void store(CreativeRequestCacheEntry entry) {
        if (entry == null || entry.id() == null) {
            return;
        }
        redisAccessSupport.write(
                PromptRedisKeys.creativeRequest(entry.id()),
                entry,
                ttlStrategy.creativeRequestTtl(),
                "creative_request_write",
                PromptRedisOperationContext.creativeRequest(entry.workspaceId(), entry.id()));
    }

    public void invalidate(UUID workspaceId, UUID requestId) {
        redisAccessSupport.delete(
                PromptRedisKeys.creativeRequest(requestId),
                "creative_request_delete",
                PromptRedisOperationContext.creativeRequest(workspaceId, requestId));
    }
}
