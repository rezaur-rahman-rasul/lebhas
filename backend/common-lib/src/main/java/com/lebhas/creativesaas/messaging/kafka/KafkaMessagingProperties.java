package com.lebhas.creativesaas.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.kafka")
public class KafkaMessagingProperties {

    private String topicPrefix = "";

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix == null ? "" : topicPrefix;
    }
}
