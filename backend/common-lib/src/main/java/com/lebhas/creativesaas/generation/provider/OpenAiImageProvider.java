package com.lebhas.creativesaas.generation.provider;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generation.application.CreativeGenerationProperties;
import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
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

public class OpenAiImageProvider implements ImageGenerationProvider {

    private final CreativeGenerationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiImageProvider(CreativeGenerationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getRequestTimeout())
                .build();
    }

    @Override
    public CreativeAiProviderType type() {
        return CreativeAiProviderType.OPENAI;
    }

    @Override
    public AiGenerationResponse generate(AiGenerationRequest request) {
        CreativeGenerationProperties.OpenAi openAi = properties.getOpenAi();
        if (!StringUtils.hasText(openAi.getApiKey())) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image API key is not configured");
        }

        String responseBody;
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(resolveUri(openAi))
                    .timeout(properties.getRequestTimeout())
                    .header("Authorization", "Bearer " + openAi.getApiKey().trim())
                    .header("Content-Type", "application/json");
            addOptionalHeader(requestBuilder, "OpenAI-Organization", openAi.getOrganization());
            addOptionalHeader(requestBuilder, "OpenAI-Project", openAi.getProject());
            HttpRequest httpRequest = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(openAi, request)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();
            if (response.statusCode() >= 400) {
                throw toProviderException(response.statusCode(), responseBody);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image provider is unavailable");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image request was interrupted");
        }

        return parseResponse(openAi, request, responseBody);
    }

    private URI resolveUri(CreativeGenerationProperties.OpenAi openAi) {
        String baseUrl = openAi.getBaseUrl().endsWith("/")
                ? openAi.getBaseUrl().substring(0, openAi.getBaseUrl().length() - 1)
                : openAi.getBaseUrl();
        String path = openAi.getImagePath().startsWith("/") ? openAi.getImagePath() : "/" + openAi.getImagePath();
        return URI.create(baseUrl + path);
    }

    private String buildRequestBody(CreativeGenerationProperties.OpenAi openAi, AiGenerationRequest request) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openAi.getModel());
        payload.put("prompt", request.prompt());
        payload.put("n", 1);
        payload.put("size", resolveSize(request));
        payload.put("output_format", request.outputFormat().extension());
        payload.put("response_format", "b64_json");
        return objectMapper.writeValueAsString(payload);
    }

    private String resolveSize(AiGenerationRequest request) {
        if (request.width() == null || request.height() == null) {
            return "1024x1024";
        }
        return request.width() + "x" + request.height();
    }

    private AiGenerationResponse parseResponse(
            CreativeGenerationProperties.OpenAi openAi,
            AiGenerationRequest request,
            String responseBody
    ) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode item = json.path("data").path(0);
            byte[] content = decodeContent(item);
            if (content.length == 0) {
                throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "OpenAI image provider returned no image content");
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            putIfPresent(metadata, "responseId", json.path("id").asText(null));
            putIfPresent(metadata, "revisedPrompt", item.path("revised_prompt").asText(null));
            putIfPresent(metadata, "created", json.path("created").asText(null));
            String model = json.path("model").asText(openAi.getModel());
            return new AiGenerationResponse(
                    CreativeAiProviderType.OPENAI.name(),
                    model,
                    json.path("id").asText(null),
                    content,
                    request.outputFormat().mimeType(),
                    request.outputFormat(),
                    request.width(),
                    request.height(),
                    null,
                    null,
                    null,
                    null,
                    metadata);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "OpenAI image response could not be parsed");
        }
    }

    private byte[] decodeContent(JsonNode item) {
        String base64 = item.path("b64_json").asText(null);
        if (StringUtils.hasText(base64)) {
            return Base64.getDecoder().decode(base64);
        }
        String url = item.path("url").asText(null);
        if (!StringUtils.hasText(url)) {
            return new byte[0];
        }
        try {
            HttpResponse<byte[]> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(properties.getRequestTimeout()).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "OpenAI image URL could not be downloaded");
            }
            return response.body();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI generated image download failed");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI generated image download was interrupted");
        }
    }

    private void addOptionalHeader(HttpRequest.Builder builder, String name, String value) {
        if (StringUtils.hasText(value)) {
            builder.header(name, value.trim());
        }
    }

    private void putIfPresent(Map<String, Object> metadata, String key, String value) {
        if (StringUtils.hasText(value)) {
            metadata.put(key, value);
        }
    }

    private BusinessException toProviderException(int statusCode, String responseBody) {
        String message = responseBody == null ? "" : responseBody.replaceAll("\\s+", " ").trim();
        if (message.length() > 240) {
            message = message.substring(0, 240);
        }
        if (statusCode == 401 || statusCode == 403) {
            return new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image provider rejected the configured credentials");
        }
        if (statusCode >= 500) {
            return new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image provider is temporarily unavailable");
        }
        return new BusinessException(ErrorCode.GENERATION_PROVIDER_REQUEST_FAILED, "OpenAI image provider rejected the request" + (message.isBlank() ? "" : ": " + message));
    }
}
