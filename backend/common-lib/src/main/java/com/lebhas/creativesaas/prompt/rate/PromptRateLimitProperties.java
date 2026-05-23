package com.lebhas.creativesaas.prompt.rate;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.prompt.rate-limit")
public class PromptRateLimitProperties {

    private int maxRequestsPerUser = 60;
    private int maxRequestsPerWorkspace = 300;
    private int maxRequestsPerIp = 120;
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
