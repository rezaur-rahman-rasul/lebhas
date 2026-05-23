package com.lebhas.creativesaas.generation.provider;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generation.application.CreativeGenerationProperties;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class StabilityImageProvider implements ImageGenerationProvider {

    private final CreativeGenerationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public StabilityImageProvider(CreativeGenerationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getRequestTimeout())
                .build();
    }

    @Override
    public CreativeAiProviderType type() {
        return CreativeAiProviderType.STABILITY;
    }

    @Override
    public AiGenerationResponse generate(AiGenerationRequest request) {
        CreativeGenerationProperties.Stability stability = properties.getStability();
        if (!StringUtils.hasText(stability.getApiKey())) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "Stability API key is not configured");
        }

        HttpResponse<byte[]> response;
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(resolveUri(stability))
                    .timeout(properties.getRequestTimeout())
                    .header("Authorization", "Bearer " + stability.getApiKey().trim())
                    .header("Accept", request.outputFormat().mimeType())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(stability, request)))
                    .build();
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw toProviderException(response.statusCode(), response.body());
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "Stability image provider is unavailable");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "Stability image request was interrupted");
        }

        byte[] content = parseContent(response, request.outputFormat().mimeType());
        if (content.length == 0) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "Stability image provider returned no image content");
        }
        return new AiGenerationResponse(
                CreativeAiProviderType.STABILITY.name(),
                stability.getModel(),
                null,
                content,
                request.outputFormat().mimeType(),
                request.outputFormat(),
                request.width(),
                request.height(),
                null,
                null,
                null,
                null,
                Map.of("model", stability.getModel()));
    }

    private URI resolveUri(CreativeGenerationProperties.Stability stability) {
        String baseUrl = stability.getBaseUrl().endsWith("/")
                ? stability.getBaseUrl().substring(0, stability.getBaseUrl().length() - 1)
                : stability.getBaseUrl();
        String path = stability.getGenerationPath().startsWith("/") ? stability.getGenerationPath() : "/" + stability.getGenerationPath();
        return URI.create(baseUrl + path);
    }

    private String buildRequestBody(CreativeGenerationProperties.Stability stability, AiGenerationRequest request) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("prompt", request.prompt());
        payload.put("model", stability.getModel());
        payload.put("output_format", request.outputFormat().extension());
        if (request.width() != null) {
            payload.put("width", request.width());
        }
        if (request.height() != null) {
            payload.put("height", request.height());
        }
        return objectMapper.writeValueAsString(payload);
    }

    private byte[] parseContent(HttpResponse<byte[]> response, String expectedMimeType) {
        String contentType = response.headers().firstValue("content-type").orElse("");
        if (contentType.toLowerCase().startsWith(expectedMimeType.toLowerCase()) || contentType.toLowerCase().startsWith("image/")) {
            return response.body();
        }
        try {
            JsonNode json = objectMapper.readTree(response.body());
            String encoded = firstText(json.path("image"), json.path("b64_json"), json.path("artifacts").path(0).path("base64"));
            return StringUtils.hasText(encoded) ? Base64.getDecoder().decode(encoded) : new byte[0];
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "Stability image response could not be parsed");
        }
    }

    private String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            String value = node.asText(null);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private BusinessException toProviderException(int statusCode, byte[] responseBody) {
        String message = responseBody == null ? "" : new String(responseBody).replaceAll("\\s+", " ").trim();
        if (message.length() > 240) {
            message = message.substring(0, 240);
        }
        if (statusCode == 401 || statusCode == 403) {
            return new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "Stability image provider rejected the configured credentials");
        }
        if (statusCode >= 500) {
            return new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "Stability image provider is temporarily unavailable");
        }
        return new BusinessException(ErrorCode.GENERATION_PROVIDER_REQUEST_FAILED, "Stability image provider rejected the request" + (message.isBlank() ? "" : ": " + message));
    }
}
