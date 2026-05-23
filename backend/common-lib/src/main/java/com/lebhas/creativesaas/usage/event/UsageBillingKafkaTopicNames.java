package com.lebhas.creativesaas.usage.event;

import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;

public class UsageBillingKafkaTopicNames {

    private final String topicPrefix;

    public UsageBillingKafkaTopicNames(String topicPrefix) {
        this.topicPrefix = topicPrefix == null ? "" : topicPrefix.trim();
    }

    public String usageUpdated() {
        return resolve(KafkaTopicConstants.USAGE_UPDATED);
    }

    public String usageSnapshotCreated() {
        return resolve(KafkaTopicConstants.USAGE_SNAPSHOT_CREATED);
    }

    public String downloadTracked() {
        return resolve(KafkaTopicConstants.DOWNLOAD_TRACKED);
    }

    public String shareAccessed() {
        return resolve(KafkaTopicConstants.SHARE_ACCESSED);
    }

    public String workspaceLimitExceeded() {
        return resolve(KafkaTopicConstants.WORKSPACE_LIMIT_EXCEEDED);
    }

    public String generationBlockedInsufficientCredits() {
        return resolve(KafkaTopicConstants.GENERATION_BLOCKED_INSUFFICIENT_CREDITS);
    }

    public String billingUsageLogged() {
        return resolve(KafkaTopicConstants.BILLING_USAGE_LOGGED);
    }

    private String resolve(String topic) {
        return topicPrefix.isBlank() ? topic : topicPrefix + topic;
    }
}
