package com.lebhas.ai.producer;

import com.lebhas.ai.event.AiCostEvent;
import com.lebhas.ai.event.AiCreativePipelineKafkaTopicNames;
import com.lebhas.ai.event.AiCreditEvent;
import com.lebhas.ai.event.AiGenerationLifecycleEvent;
import com.lebhas.ai.event.AiLayerLifecycleEvent;
import com.lebhas.ai.event.AiLayerUpdatedEvent;
import com.lebhas.ai.event.AiPipelineUpdatedEvent;
import com.lebhas.ai.event.AiProviderUpdatedEvent;
import com.lebhas.ai.event.AiRoutingResolvedEvent;
import com.lebhas.creativesaas.common.exception.KafkaPublishingException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

public class AiCreativePipelineEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AiCreativePipelineKafkaTopicNames topicNames;

    public AiCreativePipelineEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            AiCreativePipelineKafkaTopicNames topicNames
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicNames = topicNames;
    }

    public void publishPipelineUpdated(AiPipelineUpdatedEvent event) {
        publishAfterCommit(topicNames.aiPipelineUpdated(), key(event.pipelineId()), event);
    }

    public void publishLayerUpdated(AiLayerUpdatedEvent event) {
        publishAfterCommit(topicNames.aiLayerUpdated(), key(event.layerId(), event.pipelineId()), event);
    }

    public void publishProviderUpdated(AiProviderUpdatedEvent event) {
        publishAfterCommit(topicNames.aiProviderUpdated(), key(event.providerId()), event);
    }

    public void publishRoutingResolved(AiRoutingResolvedEvent event) {
        publishAfterCommit(topicNames.aiRoutingResolved(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishGenerationRequested(AiGenerationLifecycleEvent event) {
        publishAfterCommit(topicNames.aiGenerationRequested(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishGenerationStarted(AiGenerationLifecycleEvent event) {
        publishAfterCommit(topicNames.aiGenerationStarted(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishGenerationCompleted(AiGenerationLifecycleEvent event) {
        publishAfterCommit(topicNames.aiGenerationCompleted(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishGenerationFailed(AiGenerationLifecycleEvent event) {
        publishAfterCommit(topicNames.aiGenerationFailed(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishLayerStarted(AiLayerLifecycleEvent event) {
        publishAfterCommit(topicNames.aiLayerStarted(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishLayerCompleted(AiLayerLifecycleEvent event) {
        publishAfterCommit(topicNames.aiLayerCompleted(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishLayerFailed(AiLayerLifecycleEvent event) {
        publishAfterCommit(topicNames.aiLayerFailed(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishLayerFallbackUsed(AiLayerLifecycleEvent event) {
        publishAfterCommit(topicNames.aiLayerFallbackUsed(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishCostEstimated(AiCostEvent event) {
        publishAfterCommit(topicNames.aiCostEstimated(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishCostFinalized(AiCostEvent event) {
        publishAfterCommit(topicNames.aiCostFinalized(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishCreditsReserved(AiCreditEvent event) {
        publishAfterCommit(topicNames.creditsReserved(), key(event.creditReservationId(), event.creativeRequestId()), event);
    }

    public void publishCreditsFinalized(AiCreditEvent event) {
        publishAfterCommit(topicNames.creditsFinalized(), key(event.creditReservationId(), event.creativeRequestId()), event);
    }

    public void publishCreditsRefunded(AiCreditEvent event) {
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

    private String key(UUID primary) {
        return primary == null ? UUID.randomUUID().toString() : primary.toString();
    }

    private String key(UUID primary, UUID fallback) {
        return primary == null ? key(fallback) : primary.toString();
    }
}
