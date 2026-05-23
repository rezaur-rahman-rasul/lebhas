package com.lebhas.ai.provider;

import com.lebhas.ai.config.AiProviderProperties;
import com.lebhas.ai.dto.AiGenerationRequest;
import com.lebhas.ai.dto.AiGenerationResponse;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class GeminiProviderFoundation implements AiProvider {

    private final AiProviderProperties properties;

    public GeminiProviderFoundation(AiProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiProviderType type() {
        return AiProviderType.GEMINI;
    }

    @Override
    public boolean supports(AiGenerationRequest request) {
        return request.creativeType() != null && request.creativeType().isImage();
    }

    @Override
    public boolean isConfigured() {
        AiProviderProperties.Gemini gemini = properties.getGemini();
        return gemini.isEnabled() && StringUtils.hasText(gemini.getApiKey()) && StringUtils.hasText(gemini.getModel());
    }

    @Override
    public String configuredModel() {
        return properties.getGemini().getModel();
    }

    @Override
    public AiGenerationResponse generate(AiGenerationRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("foundationOnly", true);
        metadata.put("supportsImageGeneration", true);
        metadata.put("providerPath", properties.getGemini().getImagePath());
        metadata.put("configured", isConfigured());
        return AiGenerationResponse.failed(
                type(),
                providerName(),
                configuredModel(),
                "Gemini provider foundation is registered, but real generation is not implemented yet",
                metadata);
    }
}
