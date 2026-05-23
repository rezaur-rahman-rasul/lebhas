package com.lebhas.ai.event;

import com.lebhas.ai.domain.AiFailureType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiFailureLoggedEvent(
        String eventId,
        Instant occurredAt,
        UUID failureLogId,
        UUID creativeRequestId,
        UUID layerId,
        UUID providerId,
        String modelName,
        AiFailureType failureType,
        String failureReason,
        int retryAttempt,
        boolean fallbackTriggered,
        Map<String, Object> metadata
) {
    public AiFailureLoggedEvent {
        eventId = AiMonitoringEventSupport.eventId(eventId);
        occurredAt = AiMonitoringEventSupport.occurredAt(occurredAt);
        modelName = AiMonitoringEventSupport.normalize(modelName);
        failureReason = AiMonitoringEventSupport.normalize(failureReason);
        metadata = AiMonitoringEventSupport.immutable(metadata);
    }
}
