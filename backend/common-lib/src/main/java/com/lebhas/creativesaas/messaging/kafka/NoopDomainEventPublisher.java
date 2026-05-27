package com.lebhas.creativesaas.messaging.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "platform.kafka", name = "enabled", havingValue = "false")
public class NoopDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoopDomainEventPublisher.class);

    private final String applicationName;

    public NoopDomainEventPublisher(@Value("${spring.application.name:application}") String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public KafkaEventEnvelope<BaseDomainEvent> publish(String topic, BaseDomainEvent event) {
        KafkaEventEnvelope<BaseDomainEvent> envelope = KafkaEventEnvelope.of(event, applicationName);
        log.debug("Skipping Kafka event publish because platform.kafka.enabled=false topic={}", topic);
        return envelope;
    }
}
