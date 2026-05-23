package com.lebhas.creativesaas.approval;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.approval.event.ApprovalEventProducer;
import com.lebhas.creativesaas.approval.event.ApprovalWorkflowEvent;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.sharing.event.ShareLinkCreatedEvent;
import com.lebhas.creativesaas.sharing.event.ShareLinkEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ApprovalKafkaEventLayerUnitTest {

    @Test
    void shouldPublishRevisedApprovalWorkflowEventsToKafkaTopics() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        ApprovalEventProducer producer = new ApprovalEventProducer(kafkaTemplate);
        ApprovalWorkflowEvent event = event(ApprovalStatus.PENDING);

        producer.publishRequested(event);
        producer.publishApproved(event);
        producer.publishRejected(event);
        producer.publishRevisionRequested(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.APPROVAL_REQUESTED, event.workflowId().toString(), event);
        verify(kafkaTemplate).send(KafkaTopicConstants.APPROVAL_APPROVED, event.workflowId().toString(), event);
        verify(kafkaTemplate).send(KafkaTopicConstants.APPROVAL_REJECTED, event.workflowId().toString(), event);
        verify(kafkaTemplate).send(KafkaTopicConstants.APPROVAL_REVISION_REQUESTED, event.workflowId().toString(), event);
    }

    @Test
    void shouldExposeFinalDay6ApprovalAndShareTopicConstants() {
        assertThat(KafkaTopicConstants.APPROVAL_REQUESTED).isEqualTo("approval.requested");
        assertThat(KafkaTopicConstants.APPROVAL_APPROVED).isEqualTo("approval.approved");
        assertThat(KafkaTopicConstants.APPROVAL_REJECTED).isEqualTo("approval.rejected");
        assertThat(KafkaTopicConstants.APPROVAL_REVISION_REQUESTED).isEqualTo("approval.revision.requested");
        assertThat(KafkaTopicConstants.SHARE_LINK_CREATED).isEqualTo("share.link.created");
    }

    @Test
    void shouldPublishRevisedShareLinkCreatedEventToKafkaTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        ShareLinkEventProducer producer = new ShareLinkEventProducer(kafkaTemplate);
        ShareLinkCreatedEvent event = new ShareLinkCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "share-token",
                UUID.randomUUID(),
                Instant.parse("2026-05-29T00:00:00Z"),
                Instant.parse("2026-05-22T00:00:00Z"));

        producer.publishCreated(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.SHARE_LINK_CREATED, event.shareLinkId().toString(), event);
    }

    private ApprovalWorkflowEvent event(ApprovalStatus status) {
        return new ApprovalWorkflowEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                status,
                "comments",
                Instant.parse("2026-05-22T00:00:00Z"));
    }
}
