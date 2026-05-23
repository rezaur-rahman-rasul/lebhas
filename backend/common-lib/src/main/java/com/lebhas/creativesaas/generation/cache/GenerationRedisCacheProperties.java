package com.lebhas.creativesaas.generation.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "platform.generation.redis")
public class GenerationRedisCacheProperties {

    private Duration generationJobTtl = Duration.ofHours(2);
    private Duration generationLockTtl = Duration.ofSeconds(45);
    private Duration generatedVersionCountTtl = Duration.ofMinutes(15);
    private Duration workspaceQuotaTtl = Duration.ofMinutes(10);
    private Duration creditReservationTtl = Duration.ofMinutes(30);
    private Duration providerRateLimitWindow = Duration.ofMinutes(1);

    public Duration getGenerationJobTtl() {
        return generationJobTtl;
    }

    public void setGenerationJobTtl(Duration generationJobTtl) {
        this.generationJobTtl = generationJobTtl;
    }

    public Duration getGenerationLockTtl() {
        return generationLockTtl;
    }

    public void setGenerationLockTtl(Duration generationLockTtl) {
        this.generationLockTtl = generationLockTtl;
    }

    public Duration getGeneratedVersionCountTtl() {
        return generatedVersionCountTtl;
    }

    public void setGeneratedVersionCountTtl(Duration generatedVersionCountTtl) {
        this.generatedVersionCountTtl = generatedVersionCountTtl;
    }

    public Duration getWorkspaceQuotaTtl() {
        return workspaceQuotaTtl;
    }

    public void setWorkspaceQuotaTtl(Duration workspaceQuotaTtl) {
        this.workspaceQuotaTtl = workspaceQuotaTtl;
    }

    public Duration getCreditReservationTtl() {
        return creditReservationTtl;
    }

    public void setCreditReservationTtl(Duration creditReservationTtl) {
        this.creditReservationTtl = creditReservationTtl;
    }

    public Duration getProviderRateLimitWindow() {
        return providerRateLimitWindow;
    }

    public void setProviderRateLimitWindow(Duration providerRateLimitWindow) {
        this.providerRateLimitWindow = providerRateLimitWindow;
    }
}
