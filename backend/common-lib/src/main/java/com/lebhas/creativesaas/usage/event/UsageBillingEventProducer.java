package com.lebhas.creativesaas.usage.event;

import com.lebhas.creativesaas.common.exception.KafkaPublishingException;
import com.lebhas.creativesaas.messaging.kafka.KafkaMessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

public class UsageBillingEventProducer {

    private static final Logger log = LoggerFactory.getLogger(UsageBillingEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UsageBillingKafkaTopicNames topicNames;
    private final boolean kafkaEnabled;

    public UsageBillingEventProducer(KafkaTemplate<String, Object> kafkaTemplate, UsageBillingKafkaTopicNames topicNames) {
        this(kafkaTemplate, topicNames, true);
    }

    public UsageBillingEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            UsageBillingKafkaTopicNames topicNames,
            KafkaMessagingProperties kafkaMessagingProperties
    ) {
        this(kafkaTemplate, topicNames, kafkaMessagingProperties.isEnabled());
    }

    private UsageBillingEventProducer(KafkaTemplate<String, Object> kafkaTemplate, UsageBillingKafkaTopicNames topicNames, boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicNames = topicNames;
        this.kafkaEnabled = kafkaEnabled;
    }

    public void publishUsageUpdated(UsageUpdatedEventDto event) {
        publishAfterCommit(topicNames.usageUpdated(), key(event.referenceId(), event.workspaceId()), event);
    }

    public void publishUsageSnapshotCreated(UsageSnapshotCreatedEventDto event) {
        publishAfterCommit(topicNames.usageSnapshotCreated(), key(event.snapshotId(), event.workspaceId()), event);
    }

    public void publishDownloadTracked(DownloadTrackedEventDto event) {
        publishAfterCommit(topicNames.downloadTracked(), key(event.downloadUsageLogId(), event.generatedVersionId()), event);
    }

    public void publishShareAccessed(ShareAccessedEventDto event) {
        publishAfterCommit(topicNames.shareAccessed(), key(event.shareUsageLogId(), event.shareLinkId()), event);
    }

    public void publishWorkspaceLimitExceeded(WorkspaceLimitExceededEventDto event) {
        publishAfterCommit(topicNames.workspaceLimitExceeded(), key(event.referenceId(), event.workspaceId()), event);
    }

    public void publishGenerationBlockedInsufficientCredits(GenerationBlockedInsufficientCreditsEventDto event) {
        publishAfterCommit(topicNames.generationBlockedInsufficientCredits(), key(event.creativeRequestId(), event.workspaceId()), event);
    }

    public void publishBillingUsageLogged(BillingUsageLoggedEventDto event) {
        publishAfterCommit(topicNames.billingUsageLogged(), key(event.usageBillingLogId(), event.referenceId()), event);
    }

    private void publishAfterCommit(String topic, String key, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishNow(topic, key, event);
                }
            });
            return;
        }
        publishNow(topic, key, event);
    }

    private void publishNow(String topic, String key, Object event) {
        if (!kafkaEnabled) {
            log.debug("Skipping usage billing Kafka event publish because platform.kafka.enabled=false topic={}", topic);
            return;
        }
        try {
            kafkaTemplate.send(topic, key, event);
        } catch (RuntimeException exception) {
            throw new KafkaPublishingException("Failed to publish Kafka event to topic " + topic);
        }
    }

    private String key(UUID primary, UUID fallback) {
        if (primary != null) {
            return primary.toString();
        }
        if (fallback != null) {
            return fallback.toString();
        }
        return UUID.randomUUID().toString();
    }
}
