package com.lebhas.creativesaas.prompt.application.dto;

import com.lebhas.creativesaas.prompt.domain.PromptLanguage;

import java.util.List;
import java.util.UUID;

public record PromptBuilderContextView(
        UUID workspaceId,
        ProjectContext project,
        ProductContext productService,
        BrandContext brand,
        PromptReadinessView readiness
) {
    public record ProjectContext(UUID id, String name, UUID productServiceId, UUID brandId) {
    }

    public record ProductContext(UUID id, String name, UUID brandId, String category, String targetAudience) {
    }

    public record BrandContext(UUID id, String name, PromptLanguage allowedLanguage, String businessType, String brandVoice) {
    }
}
