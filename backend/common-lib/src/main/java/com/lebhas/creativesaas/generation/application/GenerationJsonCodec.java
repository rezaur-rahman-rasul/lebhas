package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class GenerationJsonCodec {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public GenerationJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object payload, String message) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.GENERATION_CONTEXT_INVALID, message);
        }
    }

    public Map<String, Object> readMap(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            return Collections.unmodifiableMap(objectMapper.readValue(payload, MAP_TYPE));
        } catch (Exception exception) {
            return Map.of("_raw", payload);
        }
    }

    public List<Map<String, Object>> readListOfMaps(String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(objectMapper.readValue(payload, LIST_OF_MAPS_TYPE));
        } catch (Exception exception) {
            return List.of(Map.of("_raw", payload));
        }
    }
}
