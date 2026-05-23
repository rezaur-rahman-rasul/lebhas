package com.lebhas.creativesaas.generation.event;

import com.lebhas.creativesaas.common.exception.KafkaPublishingException;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

public class GenerationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CreativeGenerationKafkaTopicNames topicNames;

    public GenerationEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            CreativeGenerationKafkaTopicNames topicNames
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicNames = topicNames;
    }

    public void publishJobQueued(GenerationJobQueuedEventDto event) {
        publishAfterCommit(topicNames.generationJobQueued(), key(event.generationJobId(), event.creativeRequestId()), event);
    }

    public void publishGenerationStarted(GenerationStartedEventDto event) {
        publishAfterCommit(topicNames.generationStarted(), key(event.generationJobId(), event.creativeRequestId()), event);
    }

    public void publishGenerationCompleted(GenerationCompletedEventDto event) {
        publishAfterCommit(topicNames.generationCompleted(), key(event.generationJobId(), event.creativeRequestId()), event);
    }

    public void publishGenerationFailed(GenerationFailedEventDto event) {
        publishAfterCommit(topicNames.generationFailed(), key(event.generationJobId(), event.creativeRequestId()), event);
    }

    public void publishGeneratedVersionCreated(GeneratedVersionCreatedEventDto event) {
        publishAfterCommit(topicNames.generatedVersionCreated(), key(event.generatedVersionId(), event.creativeRequestId()), event);
    }

    public void publishCreditsReserved(CreditLifecycleEventDto event) {
        publishAfterCommit(topicNames.creditsReserved(), key(event.creditReservationId(), event.creativeRequestId()), event);
    }

    public void publishCreditsFinalized(CreditLifecycleEventDto event) {
        publishAfterCommit(topicNames.creditsFinalized(), key(event.creditReservationId(), event.creativeRequestId()), event);
    }

    public void publishCreditsRefunded(CreditLifecycleEventDto event) {
        publishAfterCommit(topicNames.creditsRefunded(), key(event.creditReservationId(), event.creativeRequestId()), event);
    }

    private void publishAfterCommit(String topic, String key, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishNow(topic, key, event);
                }
            });
            return;
        }
        publishNow(topic, key, event);
    }

    private void publishNow(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event);
        } catch (RuntimeException exception) {
            throw new KafkaPublishingException("Failed to publish Kafka event to topic " + topic);
        }
    }

    private String key(UUID primary, UUID fallback) {
        if (primary != null) {
            return primary.toString();
        }
        if (fallback != null) {
            return fallback.toString();
        }
        return UUID.randomUUID().toString();
    }
}
