package com.lebhas.creativesaas.prompt.cache;

import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementView;
import com.lebhas.creativesaas.prompt.cache.dto.PromptEnhancementCacheEntry;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PromptEnhancementCacheService {

    private final PromptRedisAccessSupport redisAccessSupport;
    private final PromptRedisTtlStrategy ttlStrategy;

    public PromptEnhancementCacheService(
            PromptRedisAccessSupport redisAccessSupport,
            PromptRedisTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public String sha256(String payload) {
        String value = payload == null ? "" : payload.trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is required", exception);
        }
    }

    public Optional<PromptEnhancementCacheEntry> get(String promptHash) {
        return redisAccessSupport.read(
                PromptRedisKeys.promptEnhanced(promptHash),
                PromptEnhancementCacheEntry.class,
                "prompt_enhancement_read",
                PromptRedisOperationContext.promptHash(promptHash));
    }

    public PromptEnhancementCacheEntry getOrLoad(String promptHash, java.util.function.Supplier<PromptEnhancementCacheEntry> loader) {
        return get(promptHash).orElseGet(() -> {
            PromptEnhancementCacheEntry loaded = loader.get();
            if (loaded != null) {
                store(loaded);
            }
            return loaded;
        });
    }

    public void store(PromptEnhancementView view, String promptHash) {
        if (view == null || promptHash == null || promptHash.isBlank()) {
            return;
        }
        store(new PromptEnhancementCacheEntry(
                promptHash.trim(),
                view.enhancedPrompt(),
                view.reasoningSummary(),
                view.suggestedMissingFields(),
                view.aiProvider(),
                view.aiModel(),
                view.tokenUsage(),
                Instant.now()));
    }

    public void store(PromptEnhancementCacheEntry entry) {
        if (entry == null || entry.promptHash() == null || entry.promptHash().isBlank()) {
            return;
        }
        redisAccessSupport.write(
                PromptRedisKeys.promptEnhanced(entry.promptHash()),
                entry,
                ttlStrategy.promptEnhancementTtl(),
                "prompt_enhancement_write",
                PromptRedisOperationContext.promptHash(entry.promptHash()));
    }

    public void invalidate(String promptHash) {
        if (promptHash == null || promptHash.isBlank()) {
            return;
        }
        redisAccessSupport.delete(
                PromptRedisKeys.promptEnhanced(promptHash),
                "prompt_enhancement_delete",
                PromptRedisOperationContext.promptHash(promptHash));
    }
}
