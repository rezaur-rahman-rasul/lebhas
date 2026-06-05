package com.lebhas.creativesaas.generation.application;

import com.lebhas.ai.config.AiProviderFoundationConfiguration;
import com.lebhas.ai.creative.service.OpenAiDirectProperties;
import com.lebhas.creativesaas.generation.rate.CreativeGenerationRateLimitProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@Import(AiProviderFoundationConfiguration.class)
@EnableConfigurationProperties({
        CreativeGenerationProperties.class,
        OpenAiDirectProperties.class,
        CreativeGenerationRateLimitProperties.class
})
public class CreativeGenerationModuleConfiguration {

    @Bean
    @Qualifier("generationTaskExecutor")
    TaskExecutor generationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("generation-worker-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.initialize();
        return executor;
    }
}
