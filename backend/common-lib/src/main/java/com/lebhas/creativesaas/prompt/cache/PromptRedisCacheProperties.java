package com.lebhas.creativesaas.prompt.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "platform.prompt.redis")
public class PromptRedisCacheProperties {

    private Duration creativeRequestTtl = Duration.ofMinutes(20);
    private Duration promptEnhancementTtl = Duration.ofHours(24);
    private Duration promptTemplateTtl = Duration.ofMinutes(30);
    private Duration generationQuotaTtl = Duration.ofMinutes(10);
    private Duration requestProcessingLockTtl = Duration.ofMinutes(2);

    public Duration getCreativeRequestTtl() {
        return creativeRequestTtl;
    }

    public void setCreativeRequestTtl(Duration creativeRequestTtl) {
        this.creativeRequestTtl = creativeRequestTtl;
    }

    public Duration getPromptEnhancementTtl() {
        return promptEnhancementTtl;
    }

    public void setPromptEnhancementTtl(Duration promptEnhancementTtl) {
        this.promptEnhancementTtl = promptEnhancementTtl;
    }

    public Duration getPromptTemplateTtl() {
        return promptTemplateTtl;
    }

    public void setPromptTemplateTtl(Duration promptTemplateTtl) {
        this.promptTemplateTtl = promptTemplateTtl;
    }

    public Duration getGenerationQuotaTtl() {
        return generationQuotaTtl;
    }

    public void setGenerationQuotaTtl(Duration generationQuotaTtl) {
        this.generationQuotaTtl = generationQuotaTtl;
    }

    public Duration getRequestProcessingLockTtl() {
        return requestProcessingLockTtl;
    }

    public void setRequestProcessingLockTtl(Duration requestProcessingLockTtl) {
        this.requestProcessingLockTtl = requestProcessingLockTtl;
    }
}
