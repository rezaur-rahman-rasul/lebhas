package com.lebhas.ai.creative.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public class OpenAiDirectProperties {
    private String baseUrl = "https://api.openai.com";
    private String apiKey = "";
    private String defaultImageModel = "gpt-image-1.5";
    private String defaultOutputFormat = "png";
    private String defaultBackground = "opaque";
    private int timeoutSeconds = 120;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getDefaultImageModel() { return defaultImageModel; }
    public void setDefaultImageModel(String defaultImageModel) { this.defaultImageModel = defaultImageModel; }
    public String getDefaultOutputFormat() { return defaultOutputFormat; }
    public void setDefaultOutputFormat(String defaultOutputFormat) { this.defaultOutputFormat = defaultOutputFormat; }
    public String getDefaultBackground() { return defaultBackground; }
    public void setDefaultBackground(String defaultBackground) { this.defaultBackground = defaultBackground; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
