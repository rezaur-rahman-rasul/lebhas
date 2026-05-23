package com.lebhas.creativesaas.prompt.provider;

public interface TextAiProvider {

    AiProviderType type();

    AiResponse generate(AiRequest request);
}
