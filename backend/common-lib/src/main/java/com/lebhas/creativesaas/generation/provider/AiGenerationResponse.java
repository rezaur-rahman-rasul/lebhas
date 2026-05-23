package com.lebhas.creativesaas.generation.provider;

import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;

import java.util.Map;

public record AiGenerationResponse(
        String providerName,
        String model,
        String providerJobId,
        byte[] content,
        String mimeType,
        CreativeOutputFormat outputFormat,
        Integer width,
        Integer height,
        Long duration,
        String caption,
        String headline,
        String ctaText,
        Map<String, Object> metadata
) {
    public AiGenerationResponse {
        content = content == null ? new byte[0] : content.clone();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public long fileSize() {
        return content.length;
    }
}
