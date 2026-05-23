package com.lebhas.creativesaas.approval.event;

import com.lebhas.creativesaas.common.exception.KafkaPublishingException;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class ApprovalEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ApprovalEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRequested(ApprovalWorkflowEvent event) {
        publish(KafkaTopicConstants.APPROVAL_REQUESTED, event);
    }

    public void publishApproved(ApprovalWorkflowEvent event) {
        publish(KafkaTopicConstants.APPROVAL_APPROVED, event);
    }

    public void publishRejected(ApprovalWorkflowEvent event) {
        publish(KafkaTopicConstants.APPROVAL_REJECTED, event);
    }

    public void publishRevisionRequested(ApprovalWorkflowEvent event) {
        publish(KafkaTopicConstants.APPROVAL_REVISION_REQUESTED, event);
    }

    private void publish(String topic, ApprovalWorkflowEvent event) {
        String key = event.workflowId().toString();
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

    private void publishNow(String topic, String key, ApprovalWorkflowEvent event) {
        try {
            kafkaTemplate.send(topic, key, event);
        } catch (RuntimeException exception) {
            throw new KafkaPublishingException("Failed to publish Kafka event to topic " + topic);
        }
    }
}
