package com.lebhas.creativesaas.generation.provider;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generation.application.CreativeGenerationProperties;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CreativeGenerationRouter {

    public CreativeGenerationRouter(List<CreativeAiProvider> providers, CreativeGenerationProperties properties) {
    }

    public AiGenerationResponse generate(AiGenerationRequest request) {
        throw new BusinessException(
                ErrorCode.GENERATION_PROVIDER_UNAVAILABLE,
                "Legacy creative AI provider execution is disabled; use dynamic AI pipeline routing");
    }

    public String plannedProviderName(CreativeType creativeType) {
        return "DYNAMIC_PIPELINE";
    }

    public String plannedModelName(CreativeType creativeType) {
        return null;
    }
}
