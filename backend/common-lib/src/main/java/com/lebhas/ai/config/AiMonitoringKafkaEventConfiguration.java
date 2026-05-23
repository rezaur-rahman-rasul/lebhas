package com.lebhas.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.application.AiCostIntelligenceService;
import com.lebhas.ai.application.AiProviderHealthService;
import com.lebhas.ai.application.AiProviderMetricsService;
import com.lebhas.ai.application.GeneratedVersionQualityService;
import com.lebhas.ai.application.WorkspaceAiUsageService;
import com.lebhas.ai.cache.AiFailureCacheService;
import com.lebhas.ai.cache.AiLayerAnalyticsCacheService;
import com.lebhas.ai.cache.AiProviderHealthCacheService;
import com.lebhas.ai.cache.AiProviderMetricsCacheService;
import com.lebhas.ai.cache.AiQualityScoreCacheService;
import com.lebhas.ai.cache.WorkspaceAiUsageCacheService;
import com.lebhas.ai.consumer.AiFailureLogConsumer;
import com.lebhas.ai.consumer.AiLayerAnalyticsConsumer;
import com.lebhas.ai.consumer.AiProviderMetricsConsumer;
import com.lebhas.ai.consumer.AiQualityScoreConsumer;
import com.lebhas.ai.consumer.AiWorkspaceUsageConsumer;
import com.lebhas.ai.event.AiMonitoringKafkaTopicNames;
import com.lebhas.ai.infrastructure.persistence.AiFailureLogRepository;
import com.lebhas.ai.producer.AiMonitoringEventProducer;
import com.lebhas.creativesaas.messaging.kafka.KafkaMessagingProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class AiMonitoringKafkaEventConfiguration {

    @Bean
    AiMonitoringKafkaTopicNames aiMonitoringKafkaTopicNames(KafkaMessagingProperties kafkaMessagingProperties) {
        return new AiMonitoringKafkaTopicNames(kafkaMessagingProperties.getTopicPrefix());
    }

    @Bean
    AiMonitoringEventProducer aiMonitoringEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            AiMonitoringKafkaTopicNames topicNames
    ) {
        return new AiMonitoringEventProducer(kafkaTemplate, topicNames);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.ai.monitoring.kafka", name = "consumer-enabled", havingValue = "true")
    AiProviderMetricsConsumer aiProviderMetricsConsumer(
            ObjectMapper objectMapper,
            AiProviderMetricsService providerMetricsService,
            AiProviderHealthService providerHealthService,
            AiProviderMetricsCacheService providerMetricsCacheService,
            AiProviderHealthCacheService providerHealthCacheService,
            AiMonitoringEventProducer eventProducer
    ) {
        return new AiProviderMetricsConsumer(
                objectMapper,
                providerMetricsService,
                providerHealthService,
                providerMetricsCacheService,
                providerHealthCacheService,
                eventProducer);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.ai.monitoring.kafka", name = "consumer-enabled", havingValue = "true")
    AiLayerAnalyticsConsumer aiLayerAnalyticsConsumer(
            ObjectMapper objectMapper,
            AiCostIntelligenceService costIntelligenceService,
            AiLayerAnalyticsCacheService layerAnalyticsCacheService,
            AiMonitoringEventProducer eventProducer
    ) {
        return new AiLayerAnalyticsConsumer(objectMapper, costIntelligenceService, layerAnalyticsCacheService, eventProducer);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.ai.monitoring.kafka", name = "consumer-enabled", havingValue = "true")
    AiWorkspaceUsageConsumer aiWorkspaceUsageConsumer(
            ObjectMapper objectMapper,
            WorkspaceAiUsageService workspaceAiUsageService,
            WorkspaceAiUsageCacheService workspaceAiUsageCacheService,
            AiMonitoringEventProducer eventProducer
    ) {
        return new AiWorkspaceUsageConsumer(objectMapper, workspaceAiUsageService, workspaceAiUsageCacheService, eventProducer);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.ai.monitoring.kafka", name = "consumer-enabled", havingValue = "true")
    AiFailureLogConsumer aiFailureLogConsumer(
            ObjectMapper objectMapper,
            AiFailureLogRepository failureLogRepository,
            AiFailureCacheService failureCacheService,
            AiMonitoringEventProducer eventProducer
    ) {
        return new AiFailureLogConsumer(objectMapper, failureLogRepository, failureCacheService, eventProducer);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.ai.monitoring.kafka", name = "consumer-enabled", havingValue = "true")
    AiQualityScoreConsumer aiQualityScoreConsumer(
            ObjectMapper objectMapper,
            GeneratedVersionQualityService qualityService,
            AiQualityScoreCacheService qualityScoreCacheService
    ) {
        return new AiQualityScoreConsumer(objectMapper, qualityService, qualityScoreCacheService);
    }

    @Bean
    NewTopic aiProviderMetricsUpdatedTopic(AiMonitoringKafkaTopicNames topicNames) {
        return topic(topicNames.providerMetricsUpdated());
    }

    @Bean
    NewTopic aiProviderHealthChangedTopic(AiMonitoringKafkaTopicNames topicNames) {
        return topic(topicNames.providerHealthChanged());
    }

    @Bean
    NewTopic aiLayerAnalyticsUpdatedTopic(AiMonitoringKafkaTopicNames topicNames) {
        return topic(topicNames.layerAnalyticsUpdated());
    }

    @Bean
    NewTopic aiWorkspaceUsageUpdatedTopic(AiMonitoringKafkaTopicNames topicNames) {
        return topic(topicNames.workspaceUsageUpdated());
    }

    @Bean
    NewTopic aiQualityScoreCreatedTopic(AiMonitoringKafkaTopicNames topicNames) {
        return topic(topicNames.qualityScoreCreated());
    }

    @Bean
    NewTopic aiFailureLoggedTopic(AiMonitoringKafkaTopicNames topicNames) {
        return topic(topicNames.failureLogged());
    }

    @Bean
    NewTopic aiMonitoringCostEstimatedTopic(AiMonitoringKafkaTopicNames topicNames) {
        return topic(topicNames.costEstimated());
    }

    @Bean
    NewTopic aiRoutingOptimizationRecommendedTopic(AiMonitoringKafkaTopicNames topicNames) {
        return topic(topicNames.routingOptimizationRecommended());
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }
}
