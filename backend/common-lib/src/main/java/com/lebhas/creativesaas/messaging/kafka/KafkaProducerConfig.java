package com.lebhas.creativesaas.messaging.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(KafkaMessagingProperties.class)
public class KafkaProducerConfig {

    @Bean
    ProducerFactory<String, Object> domainEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configuration.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configuration.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configuration.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);
        configuration.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
        configuration.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 3000);
        configuration.put(ProducerConfig.RETRIES_CONFIG, 0);
        configuration.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(configuration);
    }

    @Bean
    KafkaTemplate<String, Object> domainEventKafkaTemplate(ProducerFactory<String, Object> domainEventProducerFactory) {
        return new KafkaTemplate<>(domainEventProducerFactory);
    }
}
