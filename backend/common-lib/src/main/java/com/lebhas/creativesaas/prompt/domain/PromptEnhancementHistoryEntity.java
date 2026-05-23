package com.lebhas.creativesaas.prompt.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "prompt_enhancement_history", schema = "platform")
public class PromptEnhancementHistoryEntity extends BaseEntity {

    @Column(name = "creative_request_id", nullable = false, updatable = false)
    private UUID creativeRequestId;

    @Column(name = "original_prompt", nullable = false, columnDefinition = "TEXT")
    private String originalPrompt;

    @Column(name = "enhanced_prompt", nullable = false, columnDefinition = "TEXT")
    private String enhancedPrompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "enhancement_type", nullable = false, length = 40)
    private PromptEnhancementType enhancementType;

    protected PromptEnhancementHistoryEntity() {
    }

    public static PromptEnhancementHistoryEntity create(
            UUID creativeRequestId,
            String originalPrompt,
            String enhancedPrompt,
            PromptEnhancementType enhancementType
    ) {
        PromptEnhancementHistoryEntity entity = new PromptEnhancementHistoryEntity();
        entity.creativeRequestId = require(creativeRequestId, "creativeRequestId");
        entity.originalPrompt = normalizeRequired(originalPrompt, "originalPrompt");
        entity.enhancedPrompt = normalizeRequired(enhancedPrompt, "enhancedPrompt");
        entity.enhancementType = enhancementType == null ? PromptEnhancementType.ENHANCE : enhancementType;
        return entity;
    }

    public UUID getCreativeRequestId() {
        return creativeRequestId;
    }

    public String getOriginalPrompt() {
        return originalPrompt;
    }

    public String getEnhancedPrompt() {
        return enhancedPrompt;
    }

    public PromptEnhancementType getEnhancementType() {
        return enhancementType;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
