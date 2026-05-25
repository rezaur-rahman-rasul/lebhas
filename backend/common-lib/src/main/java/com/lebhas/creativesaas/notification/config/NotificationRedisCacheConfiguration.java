package com.lebhas.creativesaas.notification.config;

import com.lebhas.notification.NotificationRedisTtlStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationRedisCacheConfiguration {

    @Bean
    public NotificationRedisTtlStrategy notificationRedisTtlStrategy() {
        return new NotificationRedisTtlStrategy();
    }
}
