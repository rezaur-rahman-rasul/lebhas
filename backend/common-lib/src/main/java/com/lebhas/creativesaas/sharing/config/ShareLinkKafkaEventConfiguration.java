package com.lebhas.creativesaas.sharing.config;

import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.sharing.event.ShareLinkEventProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class ShareLinkKafkaEventConfiguration {

    @Bean
    ShareLinkEventProducer shareLinkEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new ShareLinkEventProducer(kafkaTemplate);
    }

    @Bean
    NewTopic shareLinkCreatedTopic() {
        return TopicBuilder.name(KafkaTopicConstants.SHARE_LINK_CREATED)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
