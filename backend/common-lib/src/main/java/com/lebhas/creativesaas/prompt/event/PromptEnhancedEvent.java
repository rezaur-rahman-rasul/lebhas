package com.lebhas.creativesaas.prompt.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PromptEnhancedEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID promptHistoryId,
        UUID userId,
        String sourcePrompt,
        String enhancedPrompt,
        String aiProvider,
        String aiModel,
        Integer tokenUsage
) {
    public PromptEnhancedEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        promptHistoryId = Objects.requireNonNull(promptHistoryId, "promptHistoryId must not be null");
        userId = Objects.requireNonNull(userId, "userId must not be null");
        sourcePrompt = normalize(sourcePrompt);
        enhancedPrompt = normalize(enhancedPrompt);
        aiProvider = normalize(aiProvider);
        aiModel = normalize(aiModel);
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
}
