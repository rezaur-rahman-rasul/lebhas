package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementView;
import com.lebhas.creativesaas.prompt.application.dto.PromptRewriteView;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionsView;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Component
public class PromptResponseParser {

    private final PromptJsonCodec promptJsonCodec;

    public PromptResponseParser(PromptJsonCodec promptJsonCodec) {
        this.promptJsonCodec = promptJsonCodec;
    }

    public PromptEnhancementView parseEnhancement(String payload, String aiProvider, String aiModel, Integer tokenUsage) {
        JsonNode root = promptJsonCodec.readTree(payload, ErrorCode.PROMPT_AI_RESPONSE_INVALID, "AI provider returned malformed enhancement data");
        String enhancedPrompt = root.path("enhancedPrompt").asText(null);
        if (!StringUtils.hasText(enhancedPrompt)) {
            throw new com.lebhas.creativesaas.common.exception.BusinessException(
                    ErrorCode.PROMPT_AI_RESPONSE_INVALID,
                    "AI provider did not return an enhanced prompt");
        }
        return new PromptEnhancementView(
                enhancedPrompt.trim(),
                root.path("reasoningSummary").asText(""),
                readStringArray(root.path("suggestedMissingFields")),
                aiProvider,
                aiModel,
                tokenUsage);
    }

    public PromptRewriteView parseRewrite(String payload, String aiProvider, String aiModel, Integer tokenUsage) {
        JsonNode root = promptJsonCodec.readTree(payload, ErrorCode.PROMPT_AI_RESPONSE_INVALID, "AI provider returned malformed rewrite data");
        List<String> variations = readStringArray(root.path("variations"));
        if (variations.isEmpty()) {
            throw new com.lebhas.creativesaas.common.exception.BusinessException(
                    ErrorCode.PROMPT_AI_RESPONSE_INVALID,
                    "AI provider did not return rewrite variations");
        }
        return new PromptRewriteView(
                variations,
                root.path("reasoningSummary").asText(""),
                aiProvider,
                aiModel,
                tokenUsage);
    }

    public PromptSuggestionsView parseSuggestions(String payload, String aiProvider, String aiModel, Integer tokenUsage) {
        JsonNode root = promptJsonCodec.readTree(payload, ErrorCode.PROMPT_AI_RESPONSE_INVALID, "AI provider returned malformed suggestion data");
        return new PromptSuggestionsView(
                readStringArray(root.path("ctaSuggestions")),
                readStringArray(root.path("headlineSuggestions")),
                readStringArray(root.path("offerSuggestions")),
                readStringArray(root.path("creativeAngleSuggestions")),
                readStringArray(root.path("campaignToneSuggestions")),
                readStringArray(root.path("businessCategorySuggestions")),
                root.path("reasoningSummary").asText(""),
                aiProvider,
                aiModel,
                tokenUsage);
    }

    private List<String> readStringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String value = item.asText(null);
            if (StringUtils.hasText(value)) {
                values.add(value.trim());
            }
        });
        return List.copyOf(values);
    }
}
