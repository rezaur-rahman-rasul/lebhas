package com.lebhas.creativesaas.generation.event;

import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;

public class CreativeGenerationKafkaTopicNames {

    private final String topicPrefix;

    public CreativeGenerationKafkaTopicNames(String topicPrefix) {
        this.topicPrefix = topicPrefix == null ? "" : topicPrefix.trim();
    }

    public String creativeRequestCreated() {
        return resolve(KafkaTopicConstants.CREATIVE_REQUEST_CREATED);
    }

    public String creativeGenerationRequested() {
        return resolve(KafkaTopicConstants.CREATIVE_GENERATION_REQUESTED);
    }

    public String creativeGenerationStarted() {
        return resolve(KafkaTopicConstants.CREATIVE_GENERATION_STARTED);
    }

    public String creativeGenerationCompleted() {
        return resolve(KafkaTopicConstants.CREATIVE_GENERATION_COMPLETED);
    }

    public String creativeGenerationFailed() {
        return resolve(KafkaTopicConstants.CREATIVE_GENERATION_FAILED);
    }

    public String generationJobQueued() {
        return resolve(KafkaTopicConstants.GENERATION_JOB_QUEUED);
    }

    public String generationStarted() {
        return resolve(KafkaTopicConstants.GENERATION_STARTED);
    }

    public String generationCompleted() {
        return resolve(KafkaTopicConstants.GENERATION_COMPLETED);
    }

    public String generationFailed() {
        return resolve(KafkaTopicConstants.GENERATION_FAILED);
    }

    public String generatedVersionCreated() {
        return resolve(KafkaTopicConstants.GENERATED_VERSION_CREATED);
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

    public String promptEnhanced() {
        return resolve(KafkaTopicConstants.PROMPT_ENHANCED);
    }

    public String promptSuggestionGenerated() {
        return resolve(KafkaTopicConstants.PROMPT_SUGGESTION_GENERATED);
    }

    private String resolve(String topic) {
        return topicPrefix.isBlank() ? topic : topicPrefix + topic;
    }
}
