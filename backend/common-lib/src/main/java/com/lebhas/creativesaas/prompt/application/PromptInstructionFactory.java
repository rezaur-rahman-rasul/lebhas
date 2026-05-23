package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptRewriteCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionCommand;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;
import com.lebhas.creativesaas.prompt.provider.AiRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class PromptInstructionFactory {

    public AiRequest buildEnhancementRequest(
            PromptEnhancementCommand command,
            PromptContextAssembler.ResolvedPromptContext context
    ) {
        return new AiRequest(
                "enhance",
                """
                You are a senior creative strategist for a multi-tenant advertising SaaS platform serving Bangladesh-focused brands.
                Improve rough ad-creative prompts into production-ready prompts for generative creative workflows.
                Return JSON only with this exact shape:
                {
                  "enhancedPrompt": "string",
                  "reasoningSummary": "string",
                  "suggestedMissingFields": ["string"]
                }
                Keep the reasoningSummary concise and practical. suggestedMissingFields should only include fields that would materially improve the prompt.
                """,
                composeContextBlock(
                        command.customPrompt(),
                        command.businessType(),
                        command.campaignObjective() == null ? null : command.campaignObjective().name(),
                        command.platform() == null ? null : command.platform().name(),
                        command.creativeStyle() == null ? null : command.creativeStyle().name(),
                        command.language() == null ? null : command.language().name(),
                        command.tone() == null ? null : command.tone().name(),
                        command.targetAudience(),
                        command.offerDetails(),
                        command.ctaPreference(),
                        context,
                        "Original rough prompt"),
                0.4d,
                1200);
    }

    public AiRequest buildRewriteRequest(
            PromptRewriteCommand command,
            PromptContextAssembler.ResolvedPromptContext context
    ) {
        return new AiRequest(
                "rewrite",
                """
                You are a senior ad copy and prompt strategist.
                Rewrite existing creative prompts into stronger variants while preserving the core commercial intent.
                Return JSON only with this exact shape:
                {
                  "variations": ["string"],
                  "reasoningSummary": "string"
                }
                Produce 3 to 4 distinct variations.
                """,
                composeContextBlock(
                        command.existingPrompt(),
                        command.businessType(),
                        command.campaignObjective() == null ? null : command.campaignObjective().name(),
                        command.platform() == null ? null : command.platform().name(),
                        command.creativeStyle() == null ? null : command.creativeStyle().name(),
                        command.language() == null ? null : command.language().name(),
                        command.tone() == null ? null : command.tone().name(),
                        command.targetAudience(),
                        command.offerDetails(),
                        command.ctaPreference(),
                        context,
                        "Existing prompt"),
                0.6d,
                1100);
    }

    public AiRequest buildSuggestionRequest(
            PromptSuggestionCommand command,
            PromptContextAssembler.ResolvedPromptContext context
    ) {
        return new AiRequest(
                "suggestions",
                """
                You are a senior creative strategist generating structured prompt intelligence suggestions.
                Return JSON only with this exact shape:
                {
                  "ctaSuggestions": ["string"],
                  "headlineSuggestions": ["string"],
                  "offerSuggestions": ["string"],
                  "creativeAngleSuggestions": ["string"],
                  "campaignToneSuggestions": ["string"],
                  "businessCategorySuggestions": ["string"],
                  "reasoningSummary": "string"
                }
                Populate only the categories requested by the user context. Leave unrequested categories as empty arrays.
                Each populated list should contain 4 to 6 high-quality suggestions.
                """,
                composeContextBlock(
                        command.customPrompt(),
                        command.businessType(),
                        command.campaignObjective() == null ? null : command.campaignObjective().name(),
                        command.platform() == null ? null : command.platform().name(),
                        command.creativeStyle() == null ? null : command.creativeStyle().name(),
                        command.language() == null ? null : command.language().name(),
                        command.tone() == null ? null : command.tone().name(),
                        command.targetAudience(),
                        command.offerDetails(),
                        command.ctaPreference(),
                        context,
                        "Prompt or context brief")
                        + "\nRequested suggestion categories: " + requestedSuggestionTypes(command.suggestionTypes()),
                0.7d,
                1200);
    }

    private String composeContextBlock(
            String sourcePrompt,
            String businessType,
            String campaignObjective,
            String platform,
            String creativeStyle,
            String language,
            String tone,
            String targetAudience,
            String offerDetails,
            String ctaPreference,
            PromptContextAssembler.ResolvedPromptContext context,
            String sourceLabel
    ) {
        StringBuilder builder = new StringBuilder();
        append(builder, "Requested output language", language);
        append(builder, "Target platform", platform);
        append(builder, "Campaign objective", campaignObjective);
        append(builder, "Business type", businessType);
        append(builder, "Creative style", creativeStyle);
        append(builder, "Tone", tone);
        append(builder, "Target audience", targetAudience);
        append(builder, "Offer details", offerDetails);
        append(builder, "CTA preference", ctaPreference);
        if (context.template() != null) {
            append(builder, "Template name", context.template().getName());
            append(builder, "Template text", context.template().getTemplateText());
        }
        if (context.brandContext() != null) {
            builder.append("\nBrand context:\n");
            context.brandContext().asMap().forEach((key, value) -> builder.append("- ").append(key).append(": ").append(value).append('\n'));
        }
        if (!context.assets().isEmpty()) {
            builder.append("\nAsset metadata:\n");
            context.assets().forEach(asset -> {
                builder.append("- assetId: ").append(asset.id()).append(", fileName: ").append(asset.originalFileName());
                if (StringUtils.hasText(asset.assetCategory())) {
                    builder.append(", category: ").append(asset.assetCategory());
                }
                if (StringUtils.hasText(asset.fileType())) {
                    builder.append(", fileType: ").append(asset.fileType());
                }
                if (asset.width() != null && asset.height() != null) {
                    builder.append(", dimensions: ").append(asset.width()).append("x").append(asset.height());
                }
                if (asset.duration() != null) {
                    builder.append(", duration: ").append(asset.duration()).append("ms");
                }
                if (!asset.tags().isEmpty()) {
                    builder.append(", tags: ").append(asset.tags());
                }
                if (!asset.metadata().isEmpty()) {
                    builder.append(", metadata: ").append(asset.metadata());
                }
                builder.append('\n');
            });
        }
        append(builder, sourceLabel, sourcePrompt);
        return builder.toString().trim();
    }

    private void append(StringBuilder builder, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append(label).append(": ").append(value.trim()).append('\n');
    }

    private String requestedSuggestionTypes(Set<SuggestionType> suggestionTypes) {
        return suggestionTypes == null || suggestionTypes.isEmpty()
                ? SuggestionType.GENERAL_SUGGESTIONS.name()
                : suggestionTypes.stream().map(Enum::name).sorted().toList().toString();
    }
}
