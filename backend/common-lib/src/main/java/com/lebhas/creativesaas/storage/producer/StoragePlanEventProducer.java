package com.lebhas.creativesaas.storage.producer;

import com.lebhas.creativesaas.common.exception.KafkaPublishingException;
import com.lebhas.creativesaas.storage.event.AssetUploadBlockedByPlanEvent;
import com.lebhas.creativesaas.storage.event.AssetUploadCompletedEvent;
import com.lebhas.creativesaas.storage.event.StorageLimitExceededEvent;
import com.lebhas.creativesaas.storage.event.StoragePlanKafkaTopicNames;
import com.lebhas.creativesaas.storage.event.StorageUsageUpdatedEvent;
import com.lebhas.creativesaas.messaging.kafka.KafkaMessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class StoragePlanEventProducer {

    private static final Logger log = LoggerFactory.getLogger(StoragePlanEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StoragePlanKafkaTopicNames topicNames;
    private final KafkaMessagingProperties kafkaMessagingProperties;

    public StoragePlanEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            StoragePlanKafkaTopicNames topicNames,
            KafkaMessagingProperties kafkaMessagingProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicNames = topicNames;
        this.kafkaMessagingProperties = kafkaMessagingProperties;
    }

    public void publishStorageUsageUpdated(StorageUsageUpdatedEvent event) {
        publish(topicNames.storageUsageUpdated(), event.assetId() == null ? event.workspaceId().toString() : event.assetId().toString(), event);
    }

    public void publishStorageLimitExceeded(StorageLimitExceededEvent event) {
        publish(topicNames.storageLimitExceeded(), event.assetId() == null ? event.workspaceId().toString() : event.assetId().toString(), event);
    }

    public void publishAssetUploadBlockedByPlan(AssetUploadBlockedByPlanEvent event) {
        publish(topicNames.assetUploadBlockedByPlan(), event.assetId() == null ? event.workspaceId().toString() : event.assetId().toString(), event);
    }

    public void publishAssetUploadCompleted(AssetUploadCompletedEvent event) {
        publish(topicNames.assetUploadCompleted(), event.assetId().toString(), event);
    }

    private void publish(String topic, String key, Object event) {
        if (!kafkaMessagingProperties.isEnabled()) {
            log.debug("Skipping storage Kafka event publish because platform.kafka.enabled=false topic={}", topic);
            return;
        }
        try {
            kafkaTemplate.send(topic, key, event);
        } catch (RuntimeException exception) {
            throw new KafkaPublishingException("Failed to publish Kafka event to topic " + topic);
        }
    }
}
