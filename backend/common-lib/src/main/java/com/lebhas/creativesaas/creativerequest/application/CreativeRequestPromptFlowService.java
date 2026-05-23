package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generation.event.CreativeGenerationEventProducer;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.prompt.application.PromptHistoryService;
import com.lebhas.creativesaas.prompt.application.PromptJsonCodec;
import com.lebhas.creativesaas.prompt.cache.PromptEnhancementCacheService;
import com.lebhas.creativesaas.prompt.cache.dto.PromptEnhancementCacheEntry;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptHistoryEntity;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;
import com.lebhas.creativesaas.prompt.event.PromptEnhancedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CreativeRequestPromptFlowService {

    private final PromptEnhancementCacheService promptEnhancementCacheService;
    private final PromptHistoryService promptHistoryService;
    private final PromptJsonCodec promptJsonCodec;
    private final CreativeGenerationEventProducer creativeGenerationEventProducer;

    public CreativeRequestPromptFlowService(
            PromptEnhancementCacheService promptEnhancementCacheService,
            PromptHistoryService promptHistoryService,
            PromptJsonCodec promptJsonCodec,
            CreativeGenerationEventProducer creativeGenerationEventProducer
    ) {
        this.promptEnhancementCacheService = promptEnhancementCacheService;
        this.promptHistoryService = promptHistoryService;
        this.promptJsonCodec = promptJsonCodec;
        this.creativeGenerationEventProducer = creativeGenerationEventProducer;
    }

    public String resolveEnhancedPrompt(
            UUID workspaceId,
            BrandEntity brand,
            ProductServiceEntity productService,
            ProjectCampaignEntity projectCampaign,
            String sourcePrompt,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective objective,
            List<UUID> selectedAssetIds,
            String requestedEnhancedPrompt
    ) {
        if (StringUtils.hasText(requestedEnhancedPrompt)) {
            return requestedEnhancedPrompt.trim();
        }
        String promptHash = promptHash(
                workspaceId,
                brand,
                productService,
                projectCampaign,
                sourcePrompt,
                language,
                platform,
                objective,
                selectedAssetIds);
        return promptEnhancementCacheService.get(promptHash)
                .map(PromptEnhancementCacheEntry::enhancedPrompt)
                .filter(StringUtils::hasText)
                .orElseGet(() -> {
                    String enhancedPrompt = composePrompt(brand, productService, projectCampaign, sourcePrompt, language, platform, objective, selectedAssetIds);
                    promptEnhancementCacheService.store(new PromptEnhancementCacheEntry(
                            promptHash,
                            enhancedPrompt,
                            "Composed from brand language preference, campaign context, and selected assets.",
                            List.of(),
                            null,
                            null,
                            null,
                            Instant.now()));
                    return enhancedPrompt;
                });
    }

    public void recordPromptEnhanced(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID projectCampaignId,
            UUID userId,
            String sourcePrompt,
            String enhancedPrompt,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective objective,
            String brandContextSnapshot
    ) {
        if (!StringUtils.hasText(enhancedPrompt)) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    recordPromptEnhancedNow(
                            workspaceId,
                            creativeRequestId,
                            projectCampaignId,
                            userId,
                            sourcePrompt,
                            enhancedPrompt,
                            language,
                            platform,
                            objective,
                            brandContextSnapshot);
                }
            });
            return;
        }
        recordPromptEnhancedNow(
                workspaceId,
                creativeRequestId,
                projectCampaignId,
                userId,
                sourcePrompt,
                enhancedPrompt,
                language,
                platform,
                objective,
                brandContextSnapshot);
    }

    private void recordPromptEnhancedNow(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID projectCampaignId,
            UUID userId,
            String sourcePrompt,
            String enhancedPrompt,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective objective,
            String brandContextSnapshot
    ) {
        PromptHistoryEntity promptHistory = promptHistoryService.recordSuccess(
                workspaceId,
                projectCampaignId,
                creativeRequestId,
                userId,
                sourcePrompt,
                promptJsonCodec.write(
                        Map.of("enhancedPrompt", enhancedPrompt),
                        ErrorCode.PROMPT_CONTEXT_INVALID,
                        "Creative request prompt enhancement result could not be serialized"),
                language,
                platform,
                objective,
                null,
                brandContextSnapshot,
                SuggestionType.ENHANCEMENT,
                null,
                null,
                null);
        creativeGenerationEventProducer.publishPromptEnhanced(new PromptEnhancedEvent(
                null,
                null,
                workspaceId,
                promptHistory.getId(),
                userId,
                sourcePrompt,
                enhancedPrompt,
                null,
                null,
                null));
    }

    private String promptHash(
            UUID workspaceId,
            BrandEntity brand,
            ProductServiceEntity productService,
            ProjectCampaignEntity projectCampaign,
            String sourcePrompt,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective objective,
            List<UUID> selectedAssetIds
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspaceId", workspaceId);
        payload.put("brandId", brand.getId());
        payload.put("productServiceId", productService.getId());
        payload.put("projectCampaignId", projectCampaign.getId());
        payload.put("sourcePrompt", sourcePrompt == null ? null : sourcePrompt.trim());
        payload.put("brandLanguagePreference", brand.getLanguagePreference() == null ? null : brand.getLanguagePreference().name());
        payload.put("language", language == null ? null : language.name());
        payload.put("platform", platform == null ? null : platform.name());
        payload.put("objective", objective == null ? null : objective.name());
        payload.put("brandVoice", normalize(brand.getBrandVoice()));
        payload.put("brandTargetAudience", normalize(brand.getTargetAudience()));
        payload.put("productTargetAudience", normalize(productService.getTargetAudience()));
        payload.put("preferredCta", normalize(brand.getPreferredCta()));
        payload.put("selectedAssetIds", selectedAssetIds == null ? List.of() : List.copyOf(selectedAssetIds));
        return promptEnhancementCacheService.sha256(promptJsonCodec.write(
                payload,
                ErrorCode.PROMPT_CONTEXT_INVALID,
                "Creative request prompt cache payload could not be serialized"));
    }

    private String composePrompt(
            BrandEntity brand,
            ProductServiceEntity productService,
            ProjectCampaignEntity projectCampaign,
            String sourcePrompt,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective objective,
            List<UUID> selectedAssetIds
    ) {
        StringBuilder builder = new StringBuilder();
        append(builder, "Language", humanize(language));
        append(builder, "Platform", humanize(platform));
        append(builder, "Objective", humanize(objective));
        append(builder, "Brand voice", normalize(brand.getBrandVoice()));
        append(builder, "Target audience", firstText(productService.getTargetAudience(), brand.getTargetAudience()));
        append(builder, "CTA preference", normalize(brand.getPreferredCta()));
        append(builder, "Project campaign", projectCampaign.getName());
        if (selectedAssetIds != null && !selectedAssetIds.isEmpty()) {
            append(builder, "Selected assets", selectedAssetIds.toString());
        }
        append(builder, "Core prompt", sourcePrompt);
        return builder.toString().trim();
    }

    private void append(StringBuilder builder, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != ' ') {
            builder.append(' ');
        }
        builder.append(label).append(": ").append(value.trim()).append('.');
    }

    private String firstText(String primary, String fallback) {
        String normalized = normalize(primary);
        return normalized == null ? normalize(fallback) : normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String humanize(Enum<?> value) {
        if (value == null) {
            return null;
        }
        return value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
