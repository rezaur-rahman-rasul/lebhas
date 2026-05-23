package com.lebhas.creativesaas.prompt.event;

import com.lebhas.creativesaas.prompt.domain.SuggestionType;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record PromptSuggestionGeneratedEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID promptHistoryId,
        UUID userId,
        Set<SuggestionType> suggestionTypes,
        Map<String, java.util.List<String>> suggestions,
        String aiProvider,
        String aiModel,
        Integer tokenUsage
) {
    public PromptSuggestionGeneratedEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        promptHistoryId = Objects.requireNonNull(promptHistoryId, "promptHistoryId must not be null");
        userId = Objects.requireNonNull(userId, "userId must not be null");
        suggestionTypes = suggestionTypes == null ? Set.of() : Set.copyOf(suggestionTypes);
        suggestions = immutableCopy(suggestions);
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

    private static Map<String, java.util.List<String>> immutableCopy(Map<String, java.util.List<String>> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return Map.of();
        }
        Map<String, java.util.List<String>> copy = new LinkedHashMap<>();
        suggestions.forEach((key, value) -> copy.put(key, value == null ? java.util.List.of() : java.util.List.copyOf(value)));
        return Collections.unmodifiableMap(copy);
    }
}
