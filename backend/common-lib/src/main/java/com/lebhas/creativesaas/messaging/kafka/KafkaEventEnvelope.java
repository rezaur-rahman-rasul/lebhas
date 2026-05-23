package com.lebhas.creativesaas.messaging.kafka;

import java.time.Instant;
import java.util.UUID;

public record KafkaEventEnvelope<T>(
        String eventId,
        String eventType,
        UUID workspaceId,
        UUID aggregateId,
        Instant occurredAt,
        String producer,
        int schemaVersion,
        T payload
) {
    public static <T extends BaseDomainEvent> KafkaEventEnvelope<T> of(T event, String producer) {
        return new KafkaEventEnvelope<>(
                event.getEventId(),
                event.getEventType(),
                event.getWorkspaceId(),
                event.getAggregateId(),
                event.getOccurredAt(),
                producer,
                1,
                event);
    }
}
