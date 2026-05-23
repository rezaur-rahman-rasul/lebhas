package com.lebhas.creativesaas.sharing.event;

import com.lebhas.creativesaas.common.exception.KafkaPublishingException;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class ShareLinkEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ShareLinkEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCreated(ShareLinkCreatedEvent event) {
        String key = event.shareLinkId().toString();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishNow(key, event);
                }
            });
            return;
        }
        publishNow(key, event);
    }

    private void publishNow(String key, ShareLinkCreatedEvent event) {
        try {
            kafkaTemplate.send(KafkaTopicConstants.SHARE_LINK_CREATED, key, event);
        } catch (RuntimeException exception) {
            throw new KafkaPublishingException("Failed to publish Kafka event to topic " + KafkaTopicConstants.SHARE_LINK_CREATED);
        }
    }
}
