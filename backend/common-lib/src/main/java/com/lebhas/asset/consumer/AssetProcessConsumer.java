package com.lebhas.asset.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.asset.event.AssetProcessEvent;
import com.lebhas.asset.event.AssetVariantGenerateEvent;
import com.lebhas.asset.producer.AssetEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

public class AssetProcessConsumer {

    private static final Logger log = LoggerFactory.getLogger(AssetProcessConsumer.class);

    private final ObjectMapper objectMapper;
    private final AssetProcessHooks hooks;
    private final AssetEventProducer assetEventProducer;

    public AssetProcessConsumer(
            ObjectMapper objectMapper,
            AssetProcessHooks hooks,
            AssetEventProducer assetEventProducer
    ) {
        this.objectMapper = objectMapper;
        this.hooks = hooks;
        this.assetEventProducer = assetEventProducer;
    }

    @KafkaListener(topics = "#{@assetKafkaTopicNames.assetProcess()}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        AssetProcessEvent event = objectMapper.convertValue(payload, AssetProcessEvent.class);
        log.debug("Received asset process event workspaceId={} assetId={}", event.workspaceId(), event.assetId());

        if (event.imageOptimizationRequested()) {
            hooks.optimizeImage(event);
        }
        if (event.metadataExtractionRequested()) {
            hooks.extractMetadata(event);
        }
        if (event.aiPreprocessingRequested()) {
            hooks.preprocessForAi(event);
        }
        if (event.thumbnailGenerationRequested()) {
            assetEventProducer.publishVariantGenerate(AssetVariantGenerateEvent.thumbnailFrom(event));
        }
    }
}
