package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AssetMetadataSerializer {

    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public AssetMetadataSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Map<String, Object> metadata) {
        Map<String, Object> normalized = metadata == null ? Map.of() : new LinkedHashMap<>(metadata);
        try {
            return normalized.isEmpty() ? null : objectMapper.writeValueAsString(normalized);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_METADATA_INVALID, "Asset metadata must be valid JSON");
        }
    }

    public Map<String, Object> deserialize(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return Map.copyOf(objectMapper.readValue(metadataJson, METADATA_TYPE));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_METADATA_INVALID, "Asset metadata could not be parsed");
        }
    }
}
