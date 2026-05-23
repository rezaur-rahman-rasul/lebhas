package com.lebhas.creativesaas.generation.provider;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class GenericVideoProvider implements VideoGenerationProvider {

    @Override
    public CreativeAiProviderType type() {
        return CreativeAiProviderType.GENERIC_VIDEO;
    }

    @Override
    public AiGenerationResponse generate(AiGenerationRequest request) {
        throw new BusinessException(
                ErrorCode.GENERATION_PROVIDER_UNAVAILABLE,
                "Video generation provider is prepared but not enabled for artifact retrieval");
    }
}
