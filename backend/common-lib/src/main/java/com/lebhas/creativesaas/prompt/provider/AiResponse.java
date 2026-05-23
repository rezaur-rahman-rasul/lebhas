package com.lebhas.creativesaas.prompt.provider;

public record AiResponse(
        String provider,
        String model,
        String content,
        Integer tokenUsage
) {
}
