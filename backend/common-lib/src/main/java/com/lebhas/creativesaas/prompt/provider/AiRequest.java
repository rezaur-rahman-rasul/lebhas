package com.lebhas.creativesaas.prompt.provider;

public record AiRequest(
        String operation,
        String systemInstruction,
        String userInstruction,
        double temperature,
        int maxOutputTokens
) {
}
