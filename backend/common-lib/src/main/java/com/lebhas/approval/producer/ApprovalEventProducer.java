package com.lebhas.approval.producer;

import com.lebhas.approval.event.ApprovalKafkaTopicNames;
import com.lebhas.approval.event.ApprovalLifecycleEvent;
import com.lebhas.creativesaas.common.exception.KafkaPublishingException;
import org.springframework.kafka.core.KafkaTemplate;

public class ApprovalEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApprovalKafkaTopicNames topicNames;

    public ApprovalEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            ApprovalKafkaTopicNames topicNames
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicNames = topicNames;
    }

    public void publishRequestSubmitted(ApprovalLifecycleEvent event) {
        publish(topicNames.approvalRequestSubmitted(), event);
    }

    public void publishReviewStarted(ApprovalLifecycleEvent event) {
        publish(topicNames.approvalReviewStarted(), event);
    }

    public void publishApproved(ApprovalLifecycleEvent event) {
        publish(topicNames.approvalApproved(), event);
    }

    public void publishRejected(ApprovalLifecycleEvent event) {
        publish(topicNames.approvalRejected(), event);
    }

    public void publishChangesRequested(ApprovalLifecycleEvent event) {
        publish(topicNames.approvalChangesRequested(), event);
    }

    public void publishResubmitted(ApprovalLifecycleEvent event) {
        publish(topicNames.approvalResubmitted(), event);
    }

    public void publishAssigned(ApprovalLifecycleEvent event) {
        publish(topicNames.approvalAssigned(), event);
    }

    public void publishCommentCreated(ApprovalLifecycleEvent event) {
        publish(topicNames.approvalCommentCreated(), event);
    }

    private void publish(String topic, ApprovalLifecycleEvent event) {
        try {
            kafkaTemplate.send(topic, event.approvalRequestId().toString(), event);
        } catch (RuntimeException exception) {
            throw new KafkaPublishingException("Failed to publish Kafka event to topic " + topic);
        }
    }
}
