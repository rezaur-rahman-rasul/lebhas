package com.lebhas.ai.producer;

import com.lebhas.ai.event.AiCostEvent;
import com.lebhas.ai.event.AiFailureLoggedEvent;
import com.lebhas.ai.event.AiLayerAnalyticsUpdatedEvent;
import com.lebhas.ai.event.AiMonitoringKafkaTopicNames;
import com.lebhas.ai.event.AiProviderHealthChangedEvent;
import com.lebhas.ai.event.AiProviderMetricsUpdatedEvent;
import com.lebhas.ai.event.AiQualityScoreCreatedEvent;
import com.lebhas.ai.event.AiRoutingOptimizationRecommendedEvent;
import com.lebhas.ai.event.AiWorkspaceUsageUpdatedEvent;
import com.lebhas.creativesaas.common.exception.KafkaPublishingException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

public class AiMonitoringEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AiMonitoringKafkaTopicNames topicNames;

    public AiMonitoringEventProducer(KafkaTemplate<String, Object> kafkaTemplate, AiMonitoringKafkaTopicNames topicNames) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicNames = topicNames;
    }

    public void publishProviderMetricsUpdated(AiProviderMetricsUpdatedEvent event) {
        publishAfterCommit(topicNames.providerMetricsUpdated(), key(event.providerId()), event);
    }

    public void publishProviderHealthChanged(AiProviderHealthChangedEvent event) {
        publishAfterCommit(topicNames.providerHealthChanged(), key(event.providerId()), event);
    }

    public void publishLayerAnalyticsUpdated(AiLayerAnalyticsUpdatedEvent event) {
        publishAfterCommit(topicNames.layerAnalyticsUpdated(), key(event.layerId(), event.providerId()), event);
    }

    public void publishWorkspaceUsageUpdated(AiWorkspaceUsageUpdatedEvent event) {
        publishAfterCommit(topicNames.workspaceUsageUpdated(), key(event.workspaceId()), event);
    }

    public void publishQualityScoreCreated(AiQualityScoreCreatedEvent event) {
        publishAfterCommit(topicNames.qualityScoreCreated(), key(event.generatedVersionId(), event.workspaceId()), event);
    }

    public void publishFailureLogged(AiFailureLoggedEvent event) {
        publishAfterCommit(topicNames.failureLogged(), key(event.providerId(), event.creativeRequestId()), event);
    }

    public void publishCostEstimated(AiCostEvent event) {
        publishAfterCommit(topicNames.costEstimated(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishRoutingOptimizationRecommended(AiRoutingOptimizationRecommendedEvent event) {
        publishAfterCommit(topicNames.routingOptimizationRecommended(), key(event.workspaceId(), event.layerId()), event);
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
