package com.lebhas.creativesaas.approval.config;

import com.lebhas.approval.event.ApprovalKafkaTopicNames;
import com.lebhas.approval.producer.ApprovalEventProducer;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.messaging.kafka.KafkaMessagingProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class ApprovalKafkaEventConfiguration {

    @Bean
    ApprovalKafkaTopicNames approvalKafkaTopicNames(KafkaMessagingProperties kafkaMessagingProperties) {
        return new ApprovalKafkaTopicNames(kafkaMessagingProperties.getTopicPrefix());
    }

    @Bean
    ApprovalEventProducer approvalEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            ApprovalKafkaTopicNames approvalKafkaTopicNames
    ) {
        return new ApprovalEventProducer(kafkaTemplate, approvalKafkaTopicNames);
    }

    @Bean
    com.lebhas.creativesaas.approval.event.ApprovalEventProducer approvalWorkflowEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        return new com.lebhas.creativesaas.approval.event.ApprovalEventProducer(kafkaTemplate);
    }

    @Bean
    NewTopic approvalRequestedTopic() {
        return TopicBuilder.name(KafkaTopicConstants.APPROVAL_REQUESTED)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic approvalApprovedTopic() {
        return TopicBuilder.name(KafkaTopicConstants.APPROVAL_APPROVED)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic approvalRejectedTopic() {
        return TopicBuilder.name(KafkaTopicConstants.APPROVAL_REJECTED)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic approvalRevisionRequestedTopic() {
        return TopicBuilder.name(KafkaTopicConstants.APPROVAL_REVISION_REQUESTED)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
