package com.lebhas.ai.event;

import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.domain.ProviderType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiProviderUpdatedEvent(
        String eventId,
        Instant occurredAt,
        UUID providerId,
        String providerCode,
        String providerName,
        ProviderType providerType,
        ProviderStatus providerStatus,
        Boolean enabled,
        Boolean fallbackEligible,
        UUID changedBy,
        String changeType,
        Map<String, Object> metadata
) {
    public AiProviderUpdatedEvent {
        eventId = AiPipelineEventSupport.eventId(eventId);
        occurredAt = AiPipelineEventSupport.occurredAt(occurredAt);
        providerCode = AiPipelineEventSupport.normalize(providerCode);
        providerName = AiPipelineEventSupport.normalize(providerName);
        changeType = AiPipelineEventSupport.normalize(changeType);
        metadata = AiPipelineEventSupport.immutable(metadata);
    }
}
