package com.lebhas.asset.producer;

import com.lebhas.asset.event.AssetCleanupEvent;
import com.lebhas.asset.event.AssetDeletedEvent;
import com.lebhas.asset.event.AssetKafkaTopicNames;
import com.lebhas.asset.event.AssetProcessEvent;
import com.lebhas.asset.event.AssetUploadedEvent;
import com.lebhas.asset.event.AssetVariantGenerateEvent;
import com.lebhas.creativesaas.common.exception.KafkaPublishingException;
import org.springframework.kafka.core.KafkaTemplate;

public class AssetEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AssetKafkaTopicNames topicNames;

    public AssetEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            AssetKafkaTopicNames topicNames
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicNames = topicNames;
    }

    public void publishUploaded(AssetUploadedEvent event) {
        publish(topicNames.assetUploaded(), event.assetId().toString(), event);
    }

    public void publishProcess(AssetProcessEvent event) {
        publish(topicNames.assetProcess(), event.assetId().toString(), event);
    }

    public void publishDeleted(AssetDeletedEvent event) {
        publish(topicNames.assetDeleted(), event.assetId().toString(), event);
    }

    public void publishVariantGenerate(AssetVariantGenerateEvent event) {
        publish(topicNames.assetVariantGenerate(), event.assetId().toString(), event);
    }

    public void publishCleanup(AssetCleanupEvent event) {
        publish(topicNames.assetCleanup(), event.assetId().toString(), event);
    }

    private void publish(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event);
        } catch (RuntimeException exception) {
            throw new KafkaPublishingException("Failed to publish Kafka event to topic " + topic);
        }
    }
}
