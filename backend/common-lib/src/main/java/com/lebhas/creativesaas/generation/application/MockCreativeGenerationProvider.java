package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Component
public class MockCreativeGenerationProvider {

    public MockCreativeGenerationResult generate(GenerationExecutionContext context, Map<String, Object> layerOutputs, int versionNumber) {
        CreativeRequestEntity request = context.request();
        String hash = sha256(request.getWorkspaceId()
                + ":" + request.getId()
                + ":" + context.job().getId()
                + ":" + versionNumber
                + ":" + request.getSourcePrompt());
        String extension = resolveExtension(request.getRequestedFormat());
        String mimeType = extension.equals("mp4") ? "video/mp4" : "image/png";
        String objectKey = "generated/mock/"
                + request.getWorkspaceId()
                + "/"
                + request.getId()
                + "/"
                + context.job().getId()
                + "/v"
                + versionNumber
                + "."
                + extension;
        return MockCreativeGenerationResult.of(
                "mock-" + hash.substring(0, 16),
                objectKey,
                mimeType,
                extension,
                extension.equals("mp4") ? 1080 : 1200,
                extension.equals("mp4") ? 1920 : 1200,
                extension.equals("mp4") ? 15L : null,
                "mock-generated-output:" + hash,
                Map.of(
                        "mock", true,
                        "hash", hash,
                        "versionNumber", versionNumber,
                        "layerOutputs", layerOutputs));
    }

    private String resolveExtension(String requestedFormat) {
        if (requestedFormat != null && requestedFormat.equalsIgnoreCase("MP4")) {
            return "mp4";
        }
        return "png";
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
