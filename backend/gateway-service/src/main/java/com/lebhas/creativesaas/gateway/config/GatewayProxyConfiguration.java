package com.lebhas.creativesaas.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(GatewayRoutingProperties.class)
public class GatewayProxyConfiguration {

    @Bean
    HttpClient gatewayHttpClient(GatewayRoutingProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
    }
}
