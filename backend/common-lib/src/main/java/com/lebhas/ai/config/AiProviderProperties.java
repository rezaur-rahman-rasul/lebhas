package com.lebhas.ai.config;

import com.lebhas.ai.provider.AiProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.ai.generation")
public class AiProviderProperties {

    private AiProviderType imageProvider = AiProviderType.DISABLED;
    private AiProviderType videoProvider = AiProviderType.DISABLED;
    private Duration requestTimeout = Duration.ofSeconds(45);
    private final OpenAi openAi = new OpenAi();
    private final Gemini gemini = new Gemini();
    private final Mock mock = new Mock();
    private final Job job = new Job();

    public AiProviderType getImageProvider() {
        return imageProvider;
    }

    public void setImageProvider(AiProviderType imageProvider) {
        this.imageProvider = imageProvider;
    }

    public AiProviderType getVideoProvider() {
        return videoProvider;
    }

    public void setVideoProvider(AiProviderType videoProvider) {
        this.videoProvider = videoProvider;
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

    public Gemini getGemini() {
        return gemini;
    }

    public Mock getMock() {
        return mock;
    }

    public Job getJob() {
        return job;
    }

    public static class OpenAi {
        private boolean enabled;
        private String baseUrl = "https://api.openai.com";
        private String imagePath = "/v1/images/generations";
        private String apiKey = "";
        private String model = "gpt-image-1";
        private String organization = "";
        private String project = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
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

    public static class Gemini {
        private boolean enabled;
        private String baseUrl = "https://generativelanguage.googleapis.com";
        private String imagePath = "/v1beta/models/gemini-2.0-flash-exp:generateContent";
        private String apiKey = "";
        private String model = "gemini-2.0-flash-exp";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
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
    }

    public static class Mock {
        private boolean enabled = true;
        private String model = "mock-ai-v1";
        private String deterministicPrefix = "lebhas-mock";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getDeterministicPrefix() {
            return deterministicPrefix;
        }

        public void setDeterministicPrefix(String deterministicPrefix) {
            this.deterministicPrefix = deterministicPrefix;
        }
    }

    public static class Job {
        private int maxAttempts = 3;
        private boolean emitFailureMetadata = true;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public boolean isEmitFailureMetadata() {
            return emitFailureMetadata;
        }

        public void setEmitFailureMetadata(boolean emitFailureMetadata) {
            this.emitFailureMetadata = emitFailureMetadata;
        }
    }
}
