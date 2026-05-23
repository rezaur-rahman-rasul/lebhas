package com.lebhas.creativesaas.messaging.kafka;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class BaseDomainEvent {

    private final String eventId;
    private final String eventType;
    private final UUID workspaceId;
    private final UUID aggregateId;
    private final Instant occurredAt;
    private final Map<String, Object> attributes;

    public BaseDomainEvent(
            String eventType,
            UUID workspaceId,
            UUID aggregateId,
            Instant occurredAt,
            Map<String, Object> attributes
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.workspaceId = workspaceId;
        this.aggregateId = aggregateId;
        this.occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
