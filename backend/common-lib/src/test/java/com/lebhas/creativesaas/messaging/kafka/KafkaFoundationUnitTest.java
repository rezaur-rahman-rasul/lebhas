package com.lebhas.creativesaas.messaging.kafka;

import com.lebhas.creativesaas.common.exception.KafkaPublishingException;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaFoundationUnitTest {

    @Test
    void shouldCreateKafkaEventEnvelopeCorrectly() {
        BaseDomainEvent event = new BaseDomainEvent(
                KafkaTopicConstants.CREDIT_RESERVED,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-05-12T00:00:00Z"),
                Map.of("amount", "25.0000"));

        KafkaEventEnvelope<BaseDomainEvent> envelope = KafkaEventEnvelope.of(event, "creative-service");

        assertThat(envelope.eventId()).isEqualTo(event.getEventId());
        assertThat(envelope.eventType()).isEqualTo(KafkaTopicConstants.CREDIT_RESERVED);
        assertThat(envelope.producer()).isEqualTo("creative-service");
        assertThat(envelope.payload().getAttributes()).containsEntry("amount", "25.0000");
    }

    @Test
    void shouldPublishDomainEventPayloadStructure() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaMessagingProperties properties = new KafkaMessagingProperties();
        KafkaDomainEventPublisher publisher = new KafkaDomainEventPublisher(kafkaTemplate, properties, "creative-service");
        BaseDomainEvent event = new BaseDomainEvent(
                KafkaTopicConstants.CREATIVE_REQUEST_CREATED,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Map.of("requestName", "Launch creative"));

        KafkaEventEnvelope<BaseDomainEvent> envelope = publisher.publish(KafkaTopicConstants.CREATIVE_REQUEST_CREATED, event);

        verify(kafkaTemplate).send(KafkaTopicConstants.CREATIVE_REQUEST_CREATED, event.getAggregateId().toString(), envelope);
        assertThat(envelope.payload().getEventType()).isEqualTo(KafkaTopicConstants.CREATIVE_REQUEST_CREATED);
    }

    @Test
    void shouldExposeRequiredTopicConstants() {
        assertThat(KafkaTopicConstants.AUTH_LOGIN_SUCCESS).isEqualTo("auth.login.success");
        assertThat(KafkaTopicConstants.BRAND_CREATED).isEqualTo("brand.created");
        assertThat(KafkaTopicConstants.PRODUCT_SERVICE_CREATED).isEqualTo("product_service.created");
        assertThat(KafkaTopicConstants.PROJECT_CAMPAIGN_CREATED).isEqualTo("project_campaign.created");
        assertThat(KafkaTopicConstants.CREATIVE_REQUEST_CREATED).isEqualTo("creative.request.created");
        assertThat(KafkaTopicConstants.CREATIVE_GENERATION_REQUESTED).isEqualTo("creative.generation.requested");
        assertThat(KafkaTopicConstants.CREDIT_FINALIZED).isEqualTo("credit.finalized");
        assertThat(KafkaTopicConstants.ASSET_UPLOADED).isEqualTo("asset.uploaded");
        assertThat(KafkaTopicConstants.ASSET_PROCESS).isEqualTo("asset.process");
        assertThat(KafkaTopicConstants.ASSET_UPLOAD_BLOCKED_BY_PLAN).isEqualTo("asset.upload.blocked.by.plan");
        assertThat(KafkaTopicConstants.ASSET_VARIANT_GENERATE).isEqualTo("asset.variant.generate");
        assertThat(KafkaTopicConstants.ASSET_CLEANUP).isEqualTo("asset.cleanup");
        assertThat(KafkaTopicConstants.STORAGE_LIMIT_EXCEEDED).isEqualTo("storage.limit.exceeded");
        assertThat(KafkaTopicConstants.STORAGE_USAGE_UPDATED).isEqualTo("storage.usage.updated");
        assertThat(KafkaTopicConstants.SHARE_LINK_CREATED).isEqualTo("share.link.created");
    }

    @Test
    void shouldWrapKafkaPublishingFailures() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenThrow(new RuntimeException("kafka down"));
        KafkaMessagingProperties properties = new KafkaMessagingProperties();
        KafkaDomainEventPublisher publisher = new KafkaDomainEventPublisher(kafkaTemplate, properties, "creative-service");
        BaseDomainEvent event = new BaseDomainEvent(
                KafkaTopicConstants.DOWNLOAD_COMPLETED,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Map.of("downloadType", "PUBLIC"));

        assertThatThrownBy(() -> publisher.publish(KafkaTopicConstants.DOWNLOAD_COMPLETED, event))
                .isInstanceOf(KafkaPublishingException.class)
                .hasMessageContaining("Failed to publish Kafka event");
    }
}
