package com.lebhas.creativesaas.profile.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.messaging.kafka.KafkaMessagingProperties;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.profile.event.ProfileEventConsumer;
import com.lebhas.creativesaas.profile.event.ProfileEventConsumerHooks;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class ProfileKafkaEventConfiguration {

    @Bean
    NewTopic profileUpdatedTopic(KafkaMessagingProperties properties) {
        return topic(properties, KafkaTopicConstants.PROFILE_UPDATED);
    }

    @Bean
    NewTopic profileImageUploadRequestedTopic(KafkaMessagingProperties properties) {
        return topic(properties, KafkaTopicConstants.PROFILE_IMAGE_UPLOAD_REQUESTED);
    }

    @Bean
    NewTopic profileImageUpdatedTopic(KafkaMessagingProperties properties) {
        return topic(properties, KafkaTopicConstants.PROFILE_IMAGE_UPDATED);
    }

    @Bean
    NewTopic profileImageRemovedTopic(KafkaMessagingProperties properties) {
        return topic(properties, KafkaTopicConstants.PROFILE_IMAGE_REMOVED);
    }

    @Bean
    NewTopic profileSettingsUpdatedTopic(KafkaMessagingProperties properties) {
        return topic(properties, KafkaTopicConstants.PROFILE_SETTINGS_UPDATED);
    }

    @Bean
    NewTopic profilePasswordChangedTopic(KafkaMessagingProperties properties) {
        return topic(properties, KafkaTopicConstants.PROFILE_PASSWORD_CHANGED);
    }

    @Bean
    NewTopic profileSessionRevokedTopic(KafkaMessagingProperties properties) {
        return topic(properties, KafkaTopicConstants.PROFILE_SESSION_REVOKED);
    }

    @Bean
    NewTopic profileSecurityActivityCreatedTopic(KafkaMessagingProperties properties) {
        return topic(properties, KafkaTopicConstants.PROFILE_SECURITY_ACTIVITY_CREATED);
    }

    @Bean
    @ConditionalOnMissingBean(ProfileEventConsumerHooks.class)
    ProfileEventConsumerHooks profileEventConsumerHooks() {
        return new ProfileEventConsumerHooks() {
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.profile.kafka", name = "consumer-enabled", havingValue = "true")
    ProfileEventConsumer profileEventConsumer(ObjectMapper objectMapper, ProfileEventConsumerHooks hooks) {
        return new ProfileEventConsumer(objectMapper, hooks);
    }

    private NewTopic topic(KafkaMessagingProperties properties, String topic) {
        String prefix = properties.getTopicPrefix() == null ? "" : properties.getTopicPrefix();
        return TopicBuilder.name(prefix + topic).partitions(1).replicas(1).build();
    }
}
