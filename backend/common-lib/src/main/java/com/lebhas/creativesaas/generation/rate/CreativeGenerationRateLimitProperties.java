package com.lebhas.creativesaas.generation.rate;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.generation.rate-limit")
public class CreativeGenerationRateLimitProperties {

    private int maxRequestsPerUser = 30;
    private int maxRequestsPerWorkspace = 150;
    private int maxRequestsPerIp = 60;
    private Duration window = Duration.ofHours(1);

    public int getMaxRequestsPerUser() {
        return maxRequestsPerUser;
    }

    public void setMaxRequestsPerUser(int maxRequestsPerUser) {
        this.maxRequestsPerUser = maxRequestsPerUser;
    }

    public int getMaxRequestsPerWorkspace() {
        return maxRequestsPerWorkspace;
    }

    public void setMaxRequestsPerWorkspace(int maxRequestsPerWorkspace) {
        this.maxRequestsPerWorkspace = maxRequestsPerWorkspace;
    }

    public int getMaxRequestsPerIp() {
        return maxRequestsPerIp;
    }

    public void setMaxRequestsPerIp(int maxRequestsPerIp) {
        this.maxRequestsPerIp = maxRequestsPerIp;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }
}
