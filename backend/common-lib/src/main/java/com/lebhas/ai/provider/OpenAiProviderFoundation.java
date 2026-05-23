package com.lebhas.ai.provider;

import com.lebhas.ai.config.AiProviderProperties;
import com.lebhas.ai.dto.AiGenerationRequest;
import com.lebhas.ai.dto.AiGenerationResponse;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class OpenAiProviderFoundation implements AiProvider {

    private final AiProviderProperties properties;

    public OpenAiProviderFoundation(AiProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiProviderType type() {
        return AiProviderType.OPENAI;
    }

    @Override
    public boolean supports(AiGenerationRequest request) {
        return request.creativeType() != null && request.creativeType().isImage();
    }

    @Override
    public boolean isConfigured() {
        AiProviderProperties.OpenAi openAi = properties.getOpenAi();
        return openAi.isEnabled() && StringUtils.hasText(openAi.getApiKey()) && StringUtils.hasText(openAi.getModel());
    }

    @Override
    public String configuredModel() {
        return properties.getOpenAi().getModel();
    }

    @Override
    public AiGenerationResponse generate(AiGenerationRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("foundationOnly", true);
        metadata.put("supportsImageGeneration", true);
        metadata.put("providerPath", properties.getOpenAi().getImagePath());
        metadata.put("configured", isConfigured());
        return AiGenerationResponse.failed(
                type(),
                providerName(),
                configuredModel(),
                "OpenAI provider foundation is registered, but real generation is not implemented yet",
                metadata);
    }
}
