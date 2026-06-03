package com.lebhas.creativesaas.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "platform.gateway")
public class GatewayRoutingProperties {

    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
    private final Services services = new Services();

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Services getServices() {
        return services;
    }

    public static class Services {
        private URI auth = URI.create("http://localhost:8081");
        private URI user = URI.create("http://localhost:8082");
        private URI workspace = URI.create("http://localhost:8083");
        private URI creative = URI.create("http://localhost:8084");
        private URI billing = URI.create("http://localhost:8085");
        private URI notification = URI.create("http://localhost:8086");

        public URI getAuth() {
            return auth;
        }

        public void setAuth(URI auth) {
            this.auth = auth;
        }

        public URI getUser() {
            return user;
        }

        public void setUser(URI user) {
            this.user = user;
        }

        public URI getWorkspace() {
            return workspace;
        }

        public void setWorkspace(URI workspace) {
            this.workspace = workspace;
        }

        public URI getCreative() {
            return creative;
        }

        public void setCreative(URI creative) {
            this.creative = creative;
        }

        public URI getBilling() {
            return billing;
        }

        public void setBilling(URI billing) {
            this.billing = billing;
        }

        public URI getNotification() {
            return notification;
        }

        public void setNotification(URI notification) {
            this.notification = notification;
        }
    }
}
