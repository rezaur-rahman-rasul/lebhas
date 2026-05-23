package com.lebhas.creativesaas.generation.event;

import com.lebhas.ai.dto.AiGenerationRequest;
import com.lebhas.ai.provider.AiProviderType;
import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

public record CreativeGenerationRequestedEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID projectCampaignId,
        UUID requestedBy,
        UUID creditReservationId,
        AiProviderType providerType,
        String model,
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
    public CreativeGenerationRequestedEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        creativeRequestId = Objects.requireNonNull(creativeRequestId, "creativeRequestId must not be null");
        generatedVersionId = Objects.requireNonNull(generatedVersionId, "generatedVersionId must not be null");
        projectCampaignId = Objects.requireNonNull(projectCampaignId, "projectCampaignId must not be null");
        requestedBy = Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        model = normalize(model);
        prompt = normalize(prompt);
        brandContextSnapshot = normalize(brandContextSnapshot);
        assetContextSnapshot = normalize(assetContextSnapshot);
        generationConfig = immutableCopy(generationConfig);
    }

    public AiGenerationRequest toAiGenerationRequest() {
        return new AiGenerationRequest(
                workspaceId,
                creativeRequestId,
                generatedVersionId,
                creativeType,
                targetPlatform,
                creativeObjective,
                requestedFormat,
                language,
                prompt,
                brandContextSnapshot,
                assetContextSnapshot,
                generationConfig,
                width,
                height,
                duration);
    }

    private static String normalizeEventId(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(new TreeMap<>(value)));
    }
}
