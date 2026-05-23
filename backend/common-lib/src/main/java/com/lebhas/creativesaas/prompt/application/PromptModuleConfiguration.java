package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.prompt.provider.PromptAiProperties;
import com.lebhas.creativesaas.prompt.rate.PromptRateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        PromptAiProperties.class,
        PromptRateLimitProperties.class
})
public class PromptModuleConfiguration {
}
