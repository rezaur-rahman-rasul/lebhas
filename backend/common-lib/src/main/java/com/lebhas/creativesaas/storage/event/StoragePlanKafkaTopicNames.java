package com.lebhas.creativesaas.storage.event;

import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.util.StringUtils;

public class StoragePlanKafkaTopicNames {

    private final String topicPrefix;

    public StoragePlanKafkaTopicNames(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public String storageUsageUpdated() {
        return resolve(KafkaTopicConstants.STORAGE_USAGE_UPDATED);
    }

    public String storageLimitExceeded() {
        return resolve(KafkaTopicConstants.STORAGE_LIMIT_EXCEEDED);
    }

    public String assetUploadBlockedByPlan() {
        return resolve(KafkaTopicConstants.ASSET_UPLOAD_BLOCKED_BY_PLAN);
    }

    public String assetUploadCompleted() {
        return resolve(KafkaTopicConstants.ASSET_UPLOAD_COMPLETED);
    }

    private String resolve(String topic) {
        if (!StringUtils.hasText(topicPrefix)) {
            return topic;
        }
        return topicPrefix + topic;
    }
}
