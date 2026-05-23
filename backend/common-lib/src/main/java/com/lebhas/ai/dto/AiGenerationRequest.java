package com.lebhas.ai.dto;

import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public record AiGenerationRequest(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID jobId,
        CreativeType creativeType,
        PromptPlatform targetPlatform,
        CampaignObjective creativeObjective,
        CreativeOutputFormat requestedFormat,
        PromptLanguage language,
        String prompt,
        String brandContextSnapshot,
        String assetContextSnapshot,
        Map<String, Object> generationConfig,
        Integer width,
        Integer height,
        Long duration
) {
    public AiGenerationRequest {
        prompt = normalize(prompt);
        brandContextSnapshot = normalize(brandContextSnapshot);
        assetContextSnapshot = normalize(assetContextSnapshot);
        generationConfig = immutableCopy(generationConfig);
    }

    public String deterministicKey() {
        StringBuilder builder = new StringBuilder(512);
        builder.append(valueOf(workspaceId)).append('|')
                .append(valueOf(creativeRequestId)).append('|')
                .append(valueOf(jobId)).append('|')
                .append(valueOf(creativeType)).append('|')
                .append(valueOf(targetPlatform)).append('|')
                .append(valueOf(creativeObjective)).append('|')
                .append(valueOf(requestedFormat)).append('|')
                .append(valueOf(language)).append('|')
                .append(valueOf(prompt)).append('|')
                .append(valueOf(brandContextSnapshot)).append('|')
                .append(valueOf(assetContextSnapshot)).append('|')
                .append(valueOf(width)).append('|')
                .append(valueOf(height)).append('|')
                .append(valueOf(duration)).append('|');
        generationConfig.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> builder.append(entry.getKey())
                        .append('=')
                        .append(valueOf(entry.getValue()))
                        .append(';'));
        return builder.toString();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> generationConfig) {
        if (generationConfig == null || generationConfig.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(new TreeMap<>(generationConfig)));
    }

    private static String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
