package com.lebhas.ai.provider;

import com.lebhas.ai.config.AiProviderProperties;
import com.lebhas.ai.dto.AiGenerationRequest;
import com.lebhas.ai.dto.AiGenerationResponse;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiProviderRouter {

    private final Map<AiProviderType, AiProvider> providers = new EnumMap<>(AiProviderType.class);
    private final AiProviderProperties properties;

    public AiProviderRouter(List<AiProvider> providers, AiProviderProperties properties) {
        providers.forEach(provider -> this.providers.put(provider.type(), provider));
        this.properties = properties;
    }

    public AiGenerationResponse generate(AiGenerationRequest request) {
        AiProviderType providerType = activeProviderType(request);
        if (providerType == AiProviderType.DISABLED) {
            return failure(providerType, null, "AI provider is disabled");
        }

        AiProvider provider = providers.get(providerType);
        if (provider == null) {
            return failure(providerType, null, "AI provider foundation is not registered");
        }
        if (!provider.supports(request)) {
            return failure(providerType, provider.configuredModel(), "AI provider does not support the requested creative type");
        }
        if (!provider.isConfigured()) {
            return failure(providerType, provider.configuredModel(), "AI provider credentials are not configured");
        }

        try {
            AiGenerationResponse response = provider.generate(request);
            return response == null
                    ? failure(provider.type(), provider.configuredModel(), "AI provider returned no response")
                    : response;
        } catch (RuntimeException exception) {
            return failure(provider.type(), provider.configuredModel(), sanitizeMessage(exception));
        }
    }

    public AiProviderType activeProviderType(AiGenerationRequest request) {
        return request.creativeType() != null && request.creativeType().isVideo()
                ? properties.getVideoProvider()
                : properties.getImageProvider();
    }

    public String plannedProviderName(AiGenerationRequest request) {
        return activeProviderType(request).name();
    }

    public String plannedModelName(AiGenerationRequest request) {
        AiProvider provider = providers.get(activeProviderType(request));
        return provider == null ? null : provider.configuredModel();
    }

    private AiGenerationResponse failure(AiProviderType providerType, String model, String message) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("retryable", false);
        metadata.put("foundationOnly", true);
        return AiGenerationResponse.failed(providerType, providerType.name(), model, message, metadata);
    }

    private String sanitizeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            return "AI provider execution failed";
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
    }
}
