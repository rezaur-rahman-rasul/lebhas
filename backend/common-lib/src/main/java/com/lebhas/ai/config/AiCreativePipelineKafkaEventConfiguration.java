package com.lebhas.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.consumer.AiCreativePipelineEventConsumer;
import com.lebhas.ai.consumer.AiCreativePipelineEventHooks;
import com.lebhas.ai.event.AiCreativePipelineKafkaTopicNames;
import com.lebhas.ai.producer.AiCreativePipelineEventProducer;
import com.lebhas.creativesaas.messaging.kafka.KafkaMessagingProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class AiCreativePipelineKafkaEventConfiguration {

    @Bean
    AiCreativePipelineKafkaTopicNames aiCreativePipelineKafkaTopicNames(KafkaMessagingProperties kafkaMessagingProperties) {
        return new AiCreativePipelineKafkaTopicNames(kafkaMessagingProperties.getTopicPrefix());
    }

    @Bean
    AiCreativePipelineEventProducer aiCreativePipelineEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            AiCreativePipelineKafkaTopicNames topicNames
    ) {
        return new AiCreativePipelineEventProducer(kafkaTemplate, topicNames);
    }

    @Bean
    @ConditionalOnMissingBean(AiCreativePipelineEventHooks.class)
    AiCreativePipelineEventHooks aiCreativePipelineEventHooks() {
        return new AiCreativePipelineEventHooks() {
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.ai.pipeline.kafka", name = "consumer-enabled", havingValue = "true")
    AiCreativePipelineEventConsumer aiCreativePipelineEventConsumer(
            ObjectMapper objectMapper,
            AiCreativePipelineEventHooks hooks
    ) {
        return new AiCreativePipelineEventConsumer(objectMapper, hooks);
    }

    @Bean
    NewTopic aiPipelineUpdatedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiPipelineUpdated());
    }

    @Bean
    NewTopic aiLayerUpdatedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiLayerUpdated());
    }

    @Bean
    NewTopic aiProviderUpdatedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiProviderUpdated());
    }

    @Bean
    NewTopic aiRoutingResolvedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiRoutingResolved());
    }

    @Bean
    NewTopic aiGenerationRequestedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiGenerationRequested());
    }

    @Bean
    NewTopic aiGenerationStartedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiGenerationStarted());
    }

    @Bean
    NewTopic aiLayerStartedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiLayerStarted());
    }

    @Bean
    NewTopic aiLayerCompletedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiLayerCompleted());
    }

    @Bean
    NewTopic aiLayerFailedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiLayerFailed());
    }

    @Bean
    NewTopic aiLayerFallbackUsedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiLayerFallbackUsed());
    }

    @Bean
    NewTopic aiGenerationCompletedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiGenerationCompleted());
    }

    @Bean
    NewTopic aiGenerationFailedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiGenerationFailed());
    }

    @Bean
    NewTopic aiCostEstimatedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiCostEstimated());
    }

    @Bean
    NewTopic aiCostFinalizedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.aiCostFinalized());
    }

    @Bean
    NewTopic aiCreditsReservedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.creditsReserved());
    }

    @Bean
    NewTopic aiCreditsFinalizedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.creditsFinalized());
    }

    @Bean
    NewTopic aiCreditsRefundedTopic(AiCreativePipelineKafkaTopicNames topicNames) {
        return topic(topicNames.creditsRefunded());
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }
}
