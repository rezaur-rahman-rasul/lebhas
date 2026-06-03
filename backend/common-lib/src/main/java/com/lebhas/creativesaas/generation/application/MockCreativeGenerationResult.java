package com.lebhas.creativesaas.generation.application;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public record MockCreativeGenerationResult(
        String providerName,
        String model,
        String providerJobId,
        String objectKey,
        String mimeType,
        String fileExtension,
        int width,
        int height,
        Long duration,
        byte[] content,
        Map<String, Object> metadata
) {
    public static MockCreativeGenerationResult of(
            String providerJobId,
            String objectKey,
            String mimeType,
            String fileExtension,
            int width,
            int height,
            Long duration,
            String deterministicPayload,
            Map<String, Object> metadata
    ) {
        return new MockCreativeGenerationResult(
                "MOCK_CREATIVE_PROVIDER",
                "mock-creative-v1",
                providerJobId,
                objectKey,
                mimeType,
                fileExtension,
                width,
                height,
                duration,
                deterministicPayload.getBytes(StandardCharsets.UTF_8),
                metadata);
    }
}
