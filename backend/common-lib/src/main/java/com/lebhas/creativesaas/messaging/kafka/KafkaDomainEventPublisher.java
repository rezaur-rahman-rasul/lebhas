package com.lebhas.creativesaas.messaging.kafka;

import com.lebhas.creativesaas.common.exception.KafkaPublishingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "platform.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaMessagingProperties kafkaMessagingProperties;
    private final String applicationName;

    public KafkaDomainEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaMessagingProperties kafkaMessagingProperties,
            @Value("${spring.application.name:application}") String applicationName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaMessagingProperties = kafkaMessagingProperties;
        this.applicationName = applicationName;
    }

    @Override
    public KafkaEventEnvelope<BaseDomainEvent> publish(String topic, BaseDomainEvent event) {
        KafkaEventEnvelope<BaseDomainEvent> envelope = KafkaEventEnvelope.of(event, applicationName);
        try {
            kafkaTemplate.send(resolveTopic(topic), event.getAggregateId().toString(), envelope);
            return envelope;
        } catch (RuntimeException exception) {
            throw new KafkaPublishingException("Failed to publish Kafka event to topic " + topic);
        }
    }

    private String resolveTopic(String topic) {
        if (!StringUtils.hasText(kafkaMessagingProperties.getTopicPrefix())) {
            return topic;
        }
        return kafkaMessagingProperties.getTopicPrefix() + topic;
    }
}
