package com.lebhas.asset.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.asset.event.AssetVariantGenerateEvent;
import com.lebhas.creativesaas.asset.domain.AssetVariantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

public class AssetVariantConsumer {

    private static final Logger log = LoggerFactory.getLogger(AssetVariantConsumer.class);

    private final ObjectMapper objectMapper;
    private final AssetVariantHooks hooks;

    public AssetVariantConsumer(
            ObjectMapper objectMapper,
            AssetVariantHooks hooks
    ) {
        this.objectMapper = objectMapper;
        this.hooks = hooks;
    }

    @KafkaListener(topics = "#{@assetKafkaTopicNames.assetVariantGenerate()}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        AssetVariantGenerateEvent event = objectMapper.convertValue(payload, AssetVariantGenerateEvent.class);
        log.debug("Received asset variant event workspaceId={} assetId={} variantType={}",
                event.workspaceId(), event.assetId(), event.variantType());

        if (event.variantType() == AssetVariantType.THUMBNAIL) {
            hooks.generateThumbnail(event);
        }
    }
}
