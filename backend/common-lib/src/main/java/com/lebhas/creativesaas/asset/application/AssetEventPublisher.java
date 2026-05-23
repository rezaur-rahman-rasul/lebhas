package com.lebhas.creativesaas.asset.application;

import com.lebhas.asset.event.AssetCleanupEvent;
import com.lebhas.asset.event.AssetDeletedEvent;
import com.lebhas.asset.event.AssetProcessEvent;
import com.lebhas.asset.event.AssetUploadedEvent;
import com.lebhas.asset.producer.AssetEventProducer;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AssetEventPublisher {

    private final DomainEventPublisher domainEventPublisher;
    private final AssetEventProducer assetEventProducer;
    private final AssetActivityLogger assetActivityLogger;

    public AssetEventPublisher(
            DomainEventPublisher domainEventPublisher,
            AssetEventProducer assetEventProducer,
            AssetActivityLogger assetActivityLogger
    ) {
        this.domainEventPublisher = domainEventPublisher;
        this.assetEventProducer = assetEventProducer;
        this.assetActivityLogger = assetActivityLogger;
    }

    public void publish(String topic, UUID workspaceId, UUID assetId, Map<String, Object> attributes) {
        try {
            domainEventPublisher.publish(
                    topic,
                    new BaseDomainEvent(topic, workspaceId, assetId, Instant.now(), attributes));
        } catch (RuntimeException exception) {
            assetActivityLogger.logKafkaFailure(topic, workspaceId, assetId, exception.getMessage());
        }
    }

    public void publishUploaded(AssetEntity asset, UUID uploadSessionId) {
        publishTyped(KafkaTopicConstants.ASSET_UPLOADED, asset, () ->
                assetEventProducer.publishUploaded(AssetUploadedEvent.from(asset, uploadSessionId)));
    }

    public void publishProcess(AssetEntity asset) {
        publishTyped(KafkaTopicConstants.ASSET_PROCESS, asset, () ->
                assetEventProducer.publishProcess(AssetProcessEvent.from(asset)));
    }

    public void publishDeleted(AssetEntity asset, boolean storageReleased) {
        publishTyped(KafkaTopicConstants.ASSET_DELETED, asset, () ->
                assetEventProducer.publishDeleted(AssetDeletedEvent.from(asset, storageReleased)));
    }

    public void publishCleanup(AssetEntity asset, boolean storageReleased) {
        publishTyped(KafkaTopicConstants.ASSET_CLEANUP, asset, () ->
                assetEventProducer.publishCleanup(AssetCleanupEvent.from(asset, storageReleased)));
    }

    private void publishTyped(String topic, AssetEntity asset, Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException exception) {
            assetActivityLogger.logKafkaFailure(topic, asset.getWorkspaceId(), asset.getId(), exception.getMessage());
        }
    }
}
