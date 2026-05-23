package com.lebhas.ai.provider;

import com.lebhas.ai.dto.AiGenerationRequest;
import com.lebhas.ai.dto.AiGenerationResponse;

public interface AiProvider {

    AiProviderType type();

    boolean supports(AiGenerationRequest request);

    default boolean isConfigured() {
        return true;
    }

    default String providerName() {
        return type().name();
    }

    default String configuredModel() {
        return null;
    }

    AiGenerationResponse generate(AiGenerationRequest request);
}
