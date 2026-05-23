package com.lebhas.creativesaas.common.jpa;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@AutoConfigurationPackage(basePackages = {"com.lebhas.creativesaas", "com.lebhas.notification", "com.lebhas.pricing", "com.lebhas.ai"})
@EnableJpaRepositories(basePackages = {"com.lebhas.creativesaas", "com.lebhas.notification", "com.lebhas.pricing", "com.lebhas.ai"})
public class PlatformJpaConfiguration {
}
