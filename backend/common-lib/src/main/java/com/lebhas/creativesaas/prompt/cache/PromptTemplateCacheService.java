package com.lebhas.creativesaas.prompt.cache;

import com.lebhas.creativesaas.prompt.cache.dto.PromptTemplateCacheEntry;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateEntity;
import com.lebhas.creativesaas.prompt.infrastructure.persistence.PromptTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class PromptTemplateCacheService {

    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptRedisAccessSupport redisAccessSupport;
    private final PromptRedisTtlStrategy ttlStrategy;

    public PromptTemplateCacheService(
            PromptTemplateRepository promptTemplateRepository,
            PromptRedisAccessSupport redisAccessSupport,
            PromptRedisTtlStrategy ttlStrategy
    ) {
        this.promptTemplateRepository = promptTemplateRepository;
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<PromptTemplateCacheEntry> get(UUID workspaceId, UUID templateId) {
        return redisAccessSupport.read(
                PromptRedisKeys.promptTemplate(templateId),
                PromptTemplateCacheEntry.class,
                "prompt_template_read",
                PromptRedisOperationContext.promptTemplate(workspaceId, templateId));
    }

    public PromptTemplateCacheEntry getOrLoad(UUID workspaceId, UUID templateId) {
        return getOrLoad(workspaceId, templateId, () -> promptTemplateRepository.findByIdAndDeletedFalse(templateId)
                .map(PromptTemplateCacheEntry::from)
                .orElse(null));
    }

    public PromptTemplateCacheEntry getOrLoad(
            UUID workspaceId,
            UUID templateId,
            Supplier<PromptTemplateCacheEntry> loader
    ) {
        return get(workspaceId, templateId).orElseGet(() -> {
            PromptTemplateCacheEntry loaded = loader.get();
            if (loaded != null) {
                store(loaded);
            }
            return loaded;
        });
    }

    public void store(PromptTemplateEntity entity) {
        store(PromptTemplateCacheEntry.from(entity));
    }

    public void store(PromptTemplateCacheEntry entry) {
        if (entry == null || entry.id() == null) {
            return;
        }
        redisAccessSupport.write(
                PromptRedisKeys.promptTemplate(entry.id()),
                entry,
                ttlStrategy.promptTemplateTtl(),
                "prompt_template_write",
                PromptRedisOperationContext.promptTemplate(entry.workspaceId(), entry.id()));
    }

    public void invalidate(UUID workspaceId, UUID templateId) {
        redisAccessSupport.delete(
                PromptRedisKeys.promptTemplate(templateId),
                "prompt_template_delete",
                PromptRedisOperationContext.promptTemplate(workspaceId, templateId));
    }
}
