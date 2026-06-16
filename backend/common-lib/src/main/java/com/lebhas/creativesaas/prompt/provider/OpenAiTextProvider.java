package com.lebhas.creativesaas.prompt.provider;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiTextProvider implements TextAiProvider {

    private final PromptAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiTextProvider(PromptAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getRequestTimeout())
                .build();
    }

    @Override
    public AiProviderType type() {
        return AiProviderType.OPENAI;
    }

    @Override
    public AiResponse generate(AiRequest request) {
        PromptAiProperties.OpenAi openAi = properties.getOpenAi();
        if (!StringUtils.hasText(openAi.getApiKey())) {
            throw new BusinessException(ErrorCode.PROMPT_AI_PROVIDER_UNAVAILABLE, "OpenAI API key is not configured");
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
            throw new BusinessException(ErrorCode.PROMPT_AI_PROVIDER_UNAVAILABLE, "AI provider is unavailable");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PROMPT_AI_PROVIDER_UNAVAILABLE, "AI provider request was interrupted");
        }

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode choice = json.path("choices").path(0);
            String content = choice.path("message").path("content").asText(null);
            if (!StringUtils.hasText(content)) {
                throw new BusinessException(ErrorCode.PROMPT_AI_RESPONSE_INVALID, "AI provider returned an empty response");
            }
            JsonNode usage = json.path("usage");
            Integer totalTokens = usage.path("total_tokens").canConvertToInt() ? usage.path("total_tokens").intValue() : null;
            String model = json.path("model").asText(openAi.getModel());
            return new AiResponse(AiProviderType.OPENAI.name(), model, content, totalTokens);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.PROMPT_AI_RESPONSE_INVALID, "AI provider response could not be parsed");
        }
    }

    private URI resolveUri(PromptAiProperties.OpenAi openAi) {
        String baseUrl = openAi.getBaseUrl().endsWith("/")
                ? openAi.getBaseUrl().substring(0, openAi.getBaseUrl().length() - 1)
                : openAi.getBaseUrl();
        String chatPath = openAi.getChatPath().startsWith("/") ? openAi.getChatPath() : "/" + openAi.getChatPath();
        return URI.create(baseUrl + chatPath);
    }

    private String buildRequestBody(PromptAiProperties.OpenAi openAi, AiRequest request) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openAi.getModel());
        payload.put("temperature", request.temperature());
        payload.put("max_tokens", request.maxOutputTokens());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", request.systemInstruction()),
                Map.of("role", "user", "content", request.userInstruction())));
        return objectMapper.writeValueAsString(payload);
    }

    private void addOptionalHeader(HttpRequest.Builder builder, String name, String value) {
        if (StringUtils.hasText(value)) {
            builder.header(name, value.trim());
        }
    }

    private BusinessException toProviderException(int statusCode, String responseBody) {
        String message = responseBody == null ? "" : responseBody.replaceAll("\\s+", " ").trim();
        if (message.length() > 240) {
            message = message.substring(0, 240);
        }
        if (statusCode == 401 || statusCode == 403) {
            return new BusinessException(ErrorCode.PROMPT_AI_PROVIDER_UNAVAILABLE, "AI provider rejected the configured credentials");
        }
        if (statusCode >= 500) {
            return new BusinessException(ErrorCode.PROMPT_AI_PROVIDER_UNAVAILABLE, "AI provider is temporarily unavailable");
        }
        return new BusinessException(ErrorCode.PROMPT_AI_REQUEST_FAILED, "AI provider rejected the request" + (message.isBlank() ? "" : ": " + message));
    }
}
