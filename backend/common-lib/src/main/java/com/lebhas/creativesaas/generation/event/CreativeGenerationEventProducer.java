package com.lebhas.creativesaas.generation.event;

import com.lebhas.creativesaas.common.exception.KafkaPublishingException;
import com.lebhas.creativesaas.prompt.event.PromptEnhancedEvent;
import com.lebhas.creativesaas.prompt.event.PromptSuggestionGeneratedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class CreativeGenerationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CreativeGenerationKafkaTopicNames topicNames;

    public CreativeGenerationEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            CreativeGenerationKafkaTopicNames topicNames
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicNames = topicNames;
    }

    public void publishCreativeRequestCreated(CreativeRequestCreatedEvent event) {
        publishAfterCommit(topicNames.creativeRequestCreated(), event.creativeRequestId().toString(), event);
    }

    public void publishCreativeGenerationRequested(CreativeGenerationRequestedEvent event) {
        publishAfterCommit(topicNames.creativeGenerationRequested(), event.generatedVersionId().toString(), event);
    }

    public void publishCreativeGenerationStarted(CreativeGenerationStartedEvent event) {
        publishAfterCommit(topicNames.creativeGenerationStarted(), event.generatedVersionId().toString(), event);
    }

    public void publishCreativeGenerationCompleted(CreativeGenerationCompletedEvent event) {
        publishAfterCommit(topicNames.creativeGenerationCompleted(), event.generatedVersionId().toString(), event);
    }

    public void publishCreativeGenerationFailed(CreativeGenerationFailedEvent event) {
        publishAfterCommit(topicNames.creativeGenerationFailed(), event.generatedVersionId().toString(), event);
    }

    public void publishPromptEnhanced(PromptEnhancedEvent event) {
        publishAfterCommit(topicNames.promptEnhanced(), event.promptHistoryId().toString(), event);
    }

    public void publishPromptSuggestionGenerated(PromptSuggestionGeneratedEvent event) {
        publishAfterCommit(topicNames.promptSuggestionGenerated(), event.promptHistoryId().toString(), event);
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
}
