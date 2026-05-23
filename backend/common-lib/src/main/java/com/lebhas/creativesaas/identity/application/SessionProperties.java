package com.lebhas.creativesaas.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "platform.session")
public class SessionProperties {

    private Duration activeStateTtl = Duration.ofMinutes(30);
    private Duration supportModeTtl = Duration.ofHours(2);
    private Duration entityCacheTtl = Duration.ofMinutes(5);

    public Duration getActiveStateTtl() {
        return activeStateTtl;
    }

    public void setActiveStateTtl(Duration activeStateTtl) {
        this.activeStateTtl = activeStateTtl;
    }

    public Duration getSupportModeTtl() {
        return supportModeTtl;
    }

    public void setSupportModeTtl(Duration supportModeTtl) {
        this.supportModeTtl = supportModeTtl;
    }

    public Duration getEntityCacheTtl() {
        return entityCacheTtl;
    }

    public void setEntityCacheTtl(Duration entityCacheTtl) {
        this.entityCacheTtl = entityCacheTtl;
    }
}
