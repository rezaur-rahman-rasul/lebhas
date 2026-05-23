package com.lebhas.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "platform.ai.registry")
public class AiProviderRegistryProperties {

    private final Map<String, CredentialReference> credentials = new LinkedHashMap<>();

    public Map<String, CredentialReference> getCredentials() {
        return credentials;
    }

    public boolean hasCredentialConfiguration(String credentialConfigKey) {
        return StringUtils.hasText(credentialConfigKey) && credentials.containsKey(credentialConfigKey.trim());
    }

    public static class CredentialReference {
        private String apiKey;
        private String secretKey;
        private String baseUrl;
        private String organization;
        private String project;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getOrganization() {
            return organization;
        }

        public void setOrganization(String organization) {
            this.organization = organization;
        }

        public String getProject() {
            return project;
        }

        public void setProject(String project) {
            this.project = project;
        }
    }
}
