package com.lebhas.asset.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.asset.event.AssetCleanupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

public class AssetCleanupConsumer {

    private static final Logger log = LoggerFactory.getLogger(AssetCleanupConsumer.class);

    private final ObjectMapper objectMapper;
    private final AssetCleanupHooks hooks;

    public AssetCleanupConsumer(
            ObjectMapper objectMapper,
            AssetCleanupHooks hooks
    ) {
        this.objectMapper = objectMapper;
        this.hooks = hooks;
    }

    @KafkaListener(topics = "#{@assetKafkaTopicNames.assetCleanup()}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        AssetCleanupEvent event = objectMapper.convertValue(payload, AssetCleanupEvent.class);
        log.debug("Received asset cleanup event workspaceId={} assetId={} storageReleased={}",
                event.workspaceId(), event.assetId(), event.storageReleased());
        hooks.cleanupAssetArtifacts(event);
    }
}
