package com.lebhas.creativesaas.asset.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.asset.consumer.AssetCleanupConsumer;
import com.lebhas.asset.consumer.AssetCleanupHooks;
import com.lebhas.asset.consumer.AssetProcessConsumer;
import com.lebhas.asset.consumer.AssetProcessHooks;
import com.lebhas.asset.consumer.AssetVariantConsumer;
import com.lebhas.asset.consumer.AssetVariantHooks;
import com.lebhas.asset.event.AssetKafkaTopicNames;
import com.lebhas.asset.producer.AssetEventProducer;
import com.lebhas.creativesaas.messaging.kafka.KafkaMessagingProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class AssetKafkaEventConfiguration {

    @Bean
    AssetKafkaTopicNames assetKafkaTopicNames(KafkaMessagingProperties kafkaMessagingProperties) {
        return new AssetKafkaTopicNames(kafkaMessagingProperties.getTopicPrefix());
    }

    @Bean
    AssetEventProducer assetEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            AssetKafkaTopicNames assetKafkaTopicNames
    ) {
        return new AssetEventProducer(kafkaTemplate, assetKafkaTopicNames);
    }

    @Bean
    NewTopic assetUploadedTopic(AssetKafkaTopicNames assetKafkaTopicNames) {
        return TopicBuilder.name(assetKafkaTopicNames.assetUploaded()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic assetProcessTopic(AssetKafkaTopicNames assetKafkaTopicNames) {
        return TopicBuilder.name(assetKafkaTopicNames.assetProcess()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic assetDeletedTopic(AssetKafkaTopicNames assetKafkaTopicNames) {
        return TopicBuilder.name(assetKafkaTopicNames.assetDeleted()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic assetVariantGenerateTopic(AssetKafkaTopicNames assetKafkaTopicNames) {
        return TopicBuilder.name(assetKafkaTopicNames.assetVariantGenerate()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic assetCleanupTopic(AssetKafkaTopicNames assetKafkaTopicNames) {
        return TopicBuilder.name(assetKafkaTopicNames.assetCleanup()).partitions(1).replicas(1).build();
    }

    @Bean
    @ConditionalOnMissingBean(AssetProcessHooks.class)
    AssetProcessHooks assetProcessHooks() {
        return new AssetProcessHooks() {
        };
    }

    @Bean
    @ConditionalOnMissingBean(AssetVariantHooks.class)
    AssetVariantHooks assetVariantHooks() {
        return new AssetVariantHooks() {
        };
    }

    @Bean
    @ConditionalOnMissingBean(AssetCleanupHooks.class)
    AssetCleanupHooks assetCleanupHooks() {
        return new AssetCleanupHooks() {
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.asset.kafka", name = "consumer-enabled", havingValue = "true")
    AssetProcessConsumer assetProcessConsumer(
            ObjectMapper objectMapper,
            AssetProcessHooks assetProcessHooks,
            AssetEventProducer assetEventProducer
    ) {
        return new AssetProcessConsumer(objectMapper, assetProcessHooks, assetEventProducer);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.asset.kafka", name = "consumer-enabled", havingValue = "true")
    AssetVariantConsumer assetVariantConsumer(
            ObjectMapper objectMapper,
            AssetVariantHooks assetVariantHooks
    ) {
        return new AssetVariantConsumer(objectMapper, assetVariantHooks);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.asset.kafka", name = "consumer-enabled", havingValue = "true")
    AssetCleanupConsumer assetCleanupConsumer(
            ObjectMapper objectMapper,
            AssetCleanupHooks assetCleanupHooks
    ) {
        return new AssetCleanupConsumer(objectMapper, assetCleanupHooks);
    }
}
