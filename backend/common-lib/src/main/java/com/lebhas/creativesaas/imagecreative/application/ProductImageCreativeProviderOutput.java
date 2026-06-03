package com.lebhas.creativesaas.imagecreative.application;

import java.util.Map;

public record ProductImageCreativeProviderOutput(
        String objectKey,
        String fileName,
        String mimeType,
        String fileExtension,
        int width,
        int height,
        byte[] content,
        Map<String, Object> metadata
) {
}
