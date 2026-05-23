package com.lebhas.ai.event;

import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;

public class AiCreativePipelineKafkaTopicNames {

    private final String topicPrefix;

    public AiCreativePipelineKafkaTopicNames(String topicPrefix) {
        this.topicPrefix = topicPrefix == null ? "" : topicPrefix.trim();
    }

    public String aiPipelineUpdated() {
        return resolve(KafkaTopicConstants.AI_PIPELINE_UPDATED);
    }

    public String aiLayerUpdated() {
        return resolve(KafkaTopicConstants.AI_LAYER_UPDATED);
    }

    public String aiProviderUpdated() {
        return resolve(KafkaTopicConstants.AI_PROVIDER_UPDATED);
    }

    public String aiRoutingResolved() {
        return resolve(KafkaTopicConstants.AI_ROUTING_RESOLVED);
    }

    public String aiGenerationRequested() {
        return resolve(KafkaTopicConstants.AI_GENERATION_REQUESTED);
    }

    public String aiGenerationStarted() {
        return resolve(KafkaTopicConstants.AI_GENERATION_STARTED);
    }

    public String aiLayerStarted() {
        return resolve(KafkaTopicConstants.AI_LAYER_STARTED);
    }

    public String aiLayerCompleted() {
        return resolve(KafkaTopicConstants.AI_LAYER_COMPLETED);
    }

    public String aiLayerFailed() {
        return resolve(KafkaTopicConstants.AI_LAYER_FAILED);
    }

    public String aiLayerFallbackUsed() {
        return resolve(KafkaTopicConstants.AI_LAYER_FALLBACK_USED);
    }

    public String aiGenerationCompleted() {
        return resolve(KafkaTopicConstants.AI_GENERATION_COMPLETED);
    }

    public String aiGenerationFailed() {
        return resolve(KafkaTopicConstants.AI_GENERATION_FAILED);
    }

    public String aiCostEstimated() {
        return resolve(KafkaTopicConstants.AI_COST_ESTIMATED);
    }

    public String aiCostFinalized() {
        return resolve(KafkaTopicConstants.AI_COST_FINALIZED);
    }

    public String creditsReserved() {
        return resolve(KafkaTopicConstants.CREDITS_RESERVED);
    }

    public String creditsFinalized() {
        return resolve(KafkaTopicConstants.CREDITS_FINALIZED);
    }

    public String creditsRefunded() {
        return resolve(KafkaTopicConstants.CREDITS_REFUNDED);
    }

    private String resolve(String topic) {
        return topicPrefix.isBlank() ? topic : topicPrefix + topic;
    }
}
