package com.lebhas.ai.dto;

import com.lebhas.ai.provider.AiProviderType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record AiGenerationResponse(
        AiProviderType providerType,
        String providerName,
        String model,
        String providerJobId,
        boolean success,
        byte[] content,
        String mimeType,
        Integer width,
        Integer height,
        Long duration,
        String message,
        Map<String, Object> metadata
) {
    public AiGenerationResponse {
        content = content == null ? new byte[0] : content.clone();
        metadata = immutableCopy(metadata);
    }

    public static AiGenerationResponse success(
            AiProviderType providerType,
            String providerName,
            String model,
            String providerJobId,
            byte[] content,
            String mimeType,
            Integer width,
            Integer height,
            Long duration,
            String message,
            Map<String, Object> metadata
    ) {
        return new AiGenerationResponse(
                providerType,
                providerName,
                model,
                providerJobId,
                true,
                content,
                mimeType,
                width,
                height,
                duration,
                message,
                metadata);
    }

    public static AiGenerationResponse failed(
            AiProviderType providerType,
            String providerName,
            String model,
            String message,
            Map<String, Object> metadata
    ) {
        return new AiGenerationResponse(
                providerType,
                providerName,
                model,
                null,
                false,
                new byte[0],
                null,
                null,
                null,
                null,
                message,
                metadata);
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public boolean hasContent() {
        return content.length > 0;
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
