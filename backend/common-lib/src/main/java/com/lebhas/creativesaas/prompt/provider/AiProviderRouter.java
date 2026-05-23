package com.lebhas.creativesaas.prompt.provider;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiProviderRouter {

    public AiProviderRouter(List<TextAiProvider> providers, PromptAiProperties properties) {
    }

    public AiResponse generate(AiRequest request) {
        throw new BusinessException(
                ErrorCode.PROMPT_AI_PROVIDER_UNAVAILABLE,
                "Legacy prompt AI provider execution is disabled; use dynamic AI pipeline routing");
    }

    public String activeProviderName() {
        return "DYNAMIC_PIPELINE";
    }

    public String activeModelName() {
        return null;
    }
}
