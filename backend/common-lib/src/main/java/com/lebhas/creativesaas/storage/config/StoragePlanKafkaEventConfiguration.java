package com.lebhas.creativesaas.storage.config;

import com.lebhas.creativesaas.messaging.kafka.KafkaMessagingProperties;
import com.lebhas.creativesaas.storage.event.StoragePlanKafkaTopicNames;
import com.lebhas.creativesaas.storage.producer.StoragePlanEventProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class StoragePlanKafkaEventConfiguration {

    @Bean
    StoragePlanKafkaTopicNames storagePlanKafkaTopicNames(KafkaMessagingProperties kafkaMessagingProperties) {
        return new StoragePlanKafkaTopicNames(kafkaMessagingProperties.getTopicPrefix());
    }

    @Bean
    StoragePlanEventProducer storagePlanEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            StoragePlanKafkaTopicNames storagePlanKafkaTopicNames,
            KafkaMessagingProperties kafkaMessagingProperties
    ) {
        return new StoragePlanEventProducer(kafkaTemplate, storagePlanKafkaTopicNames, kafkaMessagingProperties);
    }

    @Bean
    NewTopic storageUsageUpdatedTopic(StoragePlanKafkaTopicNames storagePlanKafkaTopicNames) {
        return TopicBuilder.name(storagePlanKafkaTopicNames.storageUsageUpdated()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic storageLimitExceededTopic(StoragePlanKafkaTopicNames storagePlanKafkaTopicNames) {
        return TopicBuilder.name(storagePlanKafkaTopicNames.storageLimitExceeded()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic assetUploadBlockedByPlanTopic(StoragePlanKafkaTopicNames storagePlanKafkaTopicNames) {
        return TopicBuilder.name(storagePlanKafkaTopicNames.assetUploadBlockedByPlan()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic assetUploadCompletedTopic(StoragePlanKafkaTopicNames storagePlanKafkaTopicNames) {
        return TopicBuilder.name(storagePlanKafkaTopicNames.assetUploadCompleted()).partitions(1).replicas(1).build();
    }
}
