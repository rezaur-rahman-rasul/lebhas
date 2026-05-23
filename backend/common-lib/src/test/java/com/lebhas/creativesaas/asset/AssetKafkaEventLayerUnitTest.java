package com.lebhas.creativesaas.asset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lebhas.asset.consumer.AssetCleanupConsumer;
import com.lebhas.asset.consumer.AssetCleanupHooks;
import com.lebhas.asset.consumer.AssetProcessConsumer;
import com.lebhas.asset.consumer.AssetProcessHooks;
import com.lebhas.asset.consumer.AssetVariantConsumer;
import com.lebhas.asset.consumer.AssetVariantHooks;
import com.lebhas.asset.event.AssetCleanupEvent;
import com.lebhas.asset.event.AssetKafkaTopicNames;
import com.lebhas.asset.event.AssetProcessEvent;
import com.lebhas.asset.event.AssetUploadedEvent;
import com.lebhas.asset.event.AssetVariantGenerateEvent;
import com.lebhas.asset.producer.AssetEventProducer;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.asset.domain.AssetVariantType;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AssetKafkaEventLayerUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldPublishUploadedEventToKafkaTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        AssetEventProducer producer = new AssetEventProducer(kafkaTemplate, new AssetKafkaTopicNames(""));
        AssetUploadedEvent event = new AssetUploadedEvent(
                "event-1",
                Instant.parse("2026-05-15T10:00:00Z"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                AssetFileType.IMAGE,
                "image/png",
                2048L,
                "workspace/assets/file.png");

        producer.publishUploaded(event);

        verify(kafkaTemplate).send("asset.uploaded", event.assetId().toString(), event);
    }

    @Test
    void shouldInvokeProcessingHooksAndQueueVariantGeneration() {
        AssetProcessHooks processHooks = mock(AssetProcessHooks.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        AssetEventProducer producer = new AssetEventProducer(kafkaTemplate, new AssetKafkaTopicNames(""));
        AssetProcessConsumer consumer = new AssetProcessConsumer(objectMapper, processHooks, producer);
        AssetProcessEvent event = new AssetProcessEvent(
                "event-2",
                Instant.parse("2026-05-15T10:05:00Z"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AssetFileType.IMAGE,
                "image/jpeg",
                "workspace/assets/photo.jpg",
                true,
                true,
                true,
                false);

        consumer.consume(objectMapper.convertValue(event, Map.class));

        verify(processHooks).optimizeImage(event);
        verify(processHooks).extractMetadata(event);
        verify(processHooks, never()).preprocessForAi(event);
        verify(kafkaTemplate).send(
                eq("asset.variant.generate"),
                eq(event.assetId().toString()),
                argThat(payload -> payload instanceof AssetVariantGenerateEvent generated
                        && generated.assetId().equals(event.assetId())
                        && generated.projectId().equals(event.projectId())
                        && generated.workspaceId().equals(event.workspaceId())
                        && generated.variantType() == AssetVariantType.THUMBNAIL
                        && generated.fileType() == event.fileType()
                        && generated.mimeType().equals(event.mimeType())
                        && generated.sourceStorageKey().equals(event.storageKey())));
    }

    @Test
    void shouldOnlyInvokeThumbnailHookForThumbnailVariants() {
        AssetVariantHooks variantHooks = mock(AssetVariantHooks.class);
        AssetVariantConsumer consumer = new AssetVariantConsumer(objectMapper, variantHooks);
        AssetVariantGenerateEvent event = new AssetVariantGenerateEvent(
                "event-3",
                Instant.parse("2026-05-15T10:10:00Z"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AssetVariantType.THUMBNAIL,
                AssetFileType.IMAGE,
                "image/webp",
                "workspace/assets/photo.jpg");

        consumer.consume(objectMapper.convertValue(event, Map.class));

        verify(variantHooks).generateThumbnail(event);
    }

    @Test
    void shouldInvokeCleanupHook() {
        AssetCleanupHooks cleanupHooks = mock(AssetCleanupHooks.class);
        AssetCleanupConsumer consumer = new AssetCleanupConsumer(objectMapper, cleanupHooks);
        AssetCleanupEvent event = new AssetCleanupEvent(
                "event-4",
                Instant.parse("2026-05-15T10:15:00Z"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "workspace/assets/photo.jpg",
                true);

        consumer.consume(objectMapper.convertValue(event, Map.class));

        verify(cleanupHooks).cleanupAssetArtifacts(event);
    }
}
