package com.lebhas.creativesaas.messaging.kafka;

public interface DomainEventPublisher {

    KafkaEventEnvelope<BaseDomainEvent> publish(String topic, BaseDomainEvent event);
}
