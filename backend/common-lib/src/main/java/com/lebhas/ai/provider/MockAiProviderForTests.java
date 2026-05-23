package com.lebhas.ai.provider;

import com.lebhas.ai.config.AiProviderProperties;
import com.lebhas.ai.dto.AiGenerationRequest;
import com.lebhas.ai.dto.AiGenerationResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MockAiProviderForTests implements AiProvider {

    private final AiProviderProperties properties;

    public MockAiProviderForTests(AiProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiProviderType type() {
        return AiProviderType.MOCK;
    }

    @Override
    public boolean supports(AiGenerationRequest request) {
        return request != null;
    }

    @Override
    public boolean isConfigured() {
        return properties.getMock().isEnabled();
    }

    @Override
    public String configuredModel() {
        return properties.getMock().getModel();
    }

    @Override
    public AiGenerationResponse generate(AiGenerationRequest request) {
        String seed = properties.getMock().getDeterministicPrefix() + "|" + request.deterministicKey();
        String digest = sha256(seed);
        byte[] content = buildPayload(seed, digest).getBytes(StandardCharsets.UTF_8);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("foundationOnly", true);
        metadata.put("mock", true);
        metadata.put("deterministicDigest", digest);
        metadata.put("requestedFormat", request.requestedFormat() == null ? null : request.requestedFormat().name());

        return AiGenerationResponse.success(
                type(),
                providerName(),
                configuredModel(),
                "mock-" + digest.substring(0, 24),
                content,
                "text/plain; charset=UTF-8",
                request.width(),
                request.height(),
                request.duration(),
                "Deterministic mock AI generation completed",
                metadata);
    }

    private String buildPayload(String seed, String digest) {
        return "provider=MOCK\nmodel=" + configuredModel() + "\ndigest=" + digest + "\nseed=" + seed + '\n';
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                hex.append(Character.forDigit((current >> 4) & 0xF, 16));
                hex.append(Character.forDigit(current & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
