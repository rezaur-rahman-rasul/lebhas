package com.lebhas.asset.event;

import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.util.StringUtils;

public class AssetKafkaTopicNames {

    private final String topicPrefix;

    public AssetKafkaTopicNames(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public String assetUploaded() {
        return resolve(KafkaTopicConstants.ASSET_UPLOADED);
    }

    public String assetProcess() {
        return resolve(KafkaTopicConstants.ASSET_PROCESS);
    }

    public String assetDeleted() {
        return resolve(KafkaTopicConstants.ASSET_DELETED);
    }

    public String assetVariantGenerate() {
        return resolve(KafkaTopicConstants.ASSET_VARIANT_GENERATE);
    }

    public String assetCleanup() {
        return resolve(KafkaTopicConstants.ASSET_CLEANUP);
    }

    private String resolve(String topic) {
        if (!StringUtils.hasText(topicPrefix)) {
            return topic;
        }
        return topicPrefix + topic;
    }
}
