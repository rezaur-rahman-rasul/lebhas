package com.lebhas.creativesaas.generation.event;

import com.lebhas.creativesaas.messaging.kafka.KafkaMessagingProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class CreativeGenerationKafkaEventConfiguration {

    @Bean
    CreativeGenerationKafkaTopicNames creativeGenerationKafkaTopicNames(KafkaMessagingProperties kafkaMessagingProperties) {
        return new CreativeGenerationKafkaTopicNames(kafkaMessagingProperties.getTopicPrefix());
    }

    @Bean
    CreativeGenerationEventProducer creativeGenerationEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            CreativeGenerationKafkaTopicNames creativeGenerationKafkaTopicNames
    ) {
        return new CreativeGenerationEventProducer(kafkaTemplate, creativeGenerationKafkaTopicNames);
    }

    @Bean
    GenerationEventProducer generationEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            CreativeGenerationKafkaTopicNames creativeGenerationKafkaTopicNames
    ) {
        return new GenerationEventProducer(kafkaTemplate, creativeGenerationKafkaTopicNames);
    }

    @Bean
    NewTopic creativeRequestCreatedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.creativeRequestCreated()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic creativeGenerationRequestedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.creativeGenerationRequested()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic creativeGenerationStartedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.creativeGenerationStarted()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic creativeGenerationCompletedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.creativeGenerationCompleted()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic creativeGenerationFailedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.creativeGenerationFailed()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic generationJobQueuedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.generationJobQueued()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic generationStartedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.generationStarted()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic generationCompletedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.generationCompleted()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic generationFailedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.generationFailed()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic generatedVersionCreatedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.generatedVersionCreated()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic creditsReservedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.creditsReserved()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic creditsFinalizedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.creditsFinalized()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic creditsRefundedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.creditsRefunded()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic promptEnhancedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.promptEnhanced()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic promptSuggestionGeneratedTopic(CreativeGenerationKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.promptSuggestionGenerated()).partitions(1).replicas(1).build();
    }
}
