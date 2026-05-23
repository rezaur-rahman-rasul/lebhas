package com.lebhas.ai.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

final class AiMonitoringEventSupport {

    private AiMonitoringEventSupport() {
    }

    static String eventId(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
    }

    static Instant occurredAt(Instant value) {
        return value == null ? Instant.now() : value;
    }

    static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static Map<String, Object> immutable(Map<String, Object> value) {
        return value == null ? Map.of() : Map.copyOf(value);
    }
}
