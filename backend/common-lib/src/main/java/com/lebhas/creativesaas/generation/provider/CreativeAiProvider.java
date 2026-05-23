package com.lebhas.creativesaas.generation.provider;

import com.lebhas.creativesaas.generation.domain.CreativeType;

public interface CreativeAiProvider {

    CreativeAiProviderType type();

    boolean supports(CreativeType creativeType);

    AiGenerationResponse generate(AiGenerationRequest request);
}
