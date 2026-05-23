package com.lebhas.ai.event;

import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;

public class AiMonitoringKafkaTopicNames {

    private final String topicPrefix;

    public AiMonitoringKafkaTopicNames(String topicPrefix) {
        this.topicPrefix = topicPrefix == null ? "" : topicPrefix.trim();
    }

    public String providerMetricsUpdated() {
        return resolve(KafkaTopicConstants.AI_PROVIDER_METRICS_UPDATED);
    }

    public String providerHealthChanged() {
        return resolve(KafkaTopicConstants.AI_PROVIDER_HEALTH_CHANGED);
    }

    public String layerAnalyticsUpdated() {
        return resolve(KafkaTopicConstants.AI_LAYER_ANALYTICS_UPDATED);
    }

    public String workspaceUsageUpdated() {
        return resolve(KafkaTopicConstants.AI_WORKSPACE_USAGE_UPDATED);
    }

    public String qualityScoreCreated() {
        return resolve(KafkaTopicConstants.AI_QUALITY_SCORE_CREATED);
    }

    public String failureLogged() {
        return resolve(KafkaTopicConstants.AI_FAILURE_LOGGED);
    }

    public String costEstimated() {
        return resolve(KafkaTopicConstants.AI_COST_ESTIMATED);
    }

    public String routingOptimizationRecommended() {
        return resolve(KafkaTopicConstants.AI_ROUTING_OPTIMIZATION_RECOMMENDED);
    }

    private String resolve(String topic) {
        return topicPrefix.isBlank() ? topic : topicPrefix + topic;
    }
}
