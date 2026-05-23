package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class PromptJsonCodec {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public PromptJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object payload, ErrorCode errorCode, String message) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new BusinessException(errorCode, message);
        }
    }

    public JsonNode readTree(String payload, ErrorCode errorCode, String message) {
        try {
            return objectMapper.readTree(stripMarkdownFence(payload));
        } catch (Exception exception) {
            throw new BusinessException(errorCode, message);
        }
    }

    public <T> T readValue(String payload, Class<T> type, ErrorCode errorCode, String message) {
        try {
            return objectMapper.readValue(stripMarkdownFence(payload), type);
        } catch (Exception exception) {
            throw new BusinessException(errorCode, message);
        }
    }

    public Map<String, Object> readMapQuietly(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            return Map.copyOf(objectMapper.readValue(payload, MAP_TYPE));
        } catch (Exception exception) {
            return Map.of("_raw", payload);
        }
    }

    private String stripMarkdownFence(String payload) {
        if (payload == null) {
            return null;
        }
        String trimmed = payload.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineBreak = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstLineBreak < 0 || lastFence <= firstLineBreak) {
            return trimmed;
        }
        return trimmed.substring(firstLineBreak + 1, lastFence).trim();
    }
}
