package com.lebhas.creativesaas.prompt.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.ai.text")
public class PromptAiProperties {

    private AiProviderType provider = AiProviderType.DISABLED;
    private Duration requestTimeout = Duration.ofSeconds(20);
    private final OpenAi openAi = new OpenAi();

    public AiProviderType getProvider() {
        return provider;
    }

    public void setProvider(AiProviderType provider) {
        this.provider = provider;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public OpenAi getOpenAi() {
        return openAi;
    }

    public static class OpenAi {
        private String baseUrl = "https://api.openai.com";
        private String chatPath = "/v1/chat/completions";
        private String apiKey = "";
        private String model = "gpt-4.1-mini";
        private String organization = "";
        private String project = "";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getChatPath() {
            return chatPath;
        }

        public void setChatPath(String chatPath) {
            this.chatPath = chatPath;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
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
