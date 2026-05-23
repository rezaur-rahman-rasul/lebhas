package com.lebhas.creativesaas.generation.provider;

import com.lebhas.creativesaas.generation.domain.CreativeType;

public interface ImageGenerationProvider extends CreativeAiProvider {

    @Override
    default boolean supports(CreativeType creativeType) {
        return creativeType.isImage();
    }
}
