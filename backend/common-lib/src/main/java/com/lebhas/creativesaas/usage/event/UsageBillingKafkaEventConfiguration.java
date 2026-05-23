package com.lebhas.creativesaas.usage.event;

import com.lebhas.creativesaas.messaging.kafka.KafkaMessagingProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class UsageBillingKafkaEventConfiguration {

    @Bean
    UsageBillingKafkaTopicNames usageBillingKafkaTopicNames(KafkaMessagingProperties kafkaMessagingProperties) {
        return new UsageBillingKafkaTopicNames(kafkaMessagingProperties.getTopicPrefix());
    }

    @Bean
    UsageBillingEventProducer usageBillingEventProducer(KafkaTemplate<String, Object> kafkaTemplate, UsageBillingKafkaTopicNames topicNames) {
        return new UsageBillingEventProducer(kafkaTemplate, topicNames);
    }

    @Bean
    NewTopic usageUpdatedTopic(UsageBillingKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.usageUpdated()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic usageSnapshotCreatedTopic(UsageBillingKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.usageSnapshotCreated()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic downloadTrackedTopic(UsageBillingKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.downloadTracked()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic shareAccessedTopic(UsageBillingKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.shareAccessed()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic workspaceLimitExceededTopic(UsageBillingKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.workspaceLimitExceeded()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic generationBlockedInsufficientCreditsTopic(UsageBillingKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.generationBlockedInsufficientCredits()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic billingUsageLoggedTopic(UsageBillingKafkaTopicNames topicNames) {
        return TopicBuilder.name(topicNames.billingUsageLogged()).partitions(1).replicas(1).build();
    }
}
