package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ai_models", schema = "platform")
public class AiModel extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "model_code", nullable = false, length = 120)
    private String modelCode;

    @Column(name = "model_name", nullable = false, length = 180)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProviderStatus status;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "default_model", nullable = false)
    private boolean defaultModel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities", nullable = false, columnDefinition = "jsonb")
    private List<String> capabilities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cost_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> costMetadata = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quality_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> qualityMetadata = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rate_limit_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rateLimitMetadata = new LinkedHashMap<>();

    protected AiModel() {
    }

    public static AiModel create(
            UUID providerId,
            String modelCode,
            String modelName,
            ProviderStatus status,
            boolean enabled,
            boolean defaultModel,
            Collection<String> capabilities,
            Map<String, Object> costMetadata,
            Map<String, Object> qualityMetadata,
            Map<String, Object> rateLimitMetadata
    ) {
        AiModel model = new AiModel();
        model.providerId = AiToolProvider.require(providerId, "providerId");
        model.apply(modelCode, modelName, status, enabled, defaultModel, capabilities, costMetadata, qualityMetadata, rateLimitMetadata);
        return model;
    }

    public void update(
            String modelName,
            ProviderStatus status,
            boolean enabled,
            boolean defaultModel,
            Collection<String> capabilities,
            Map<String, Object> costMetadata,
            Map<String, Object> qualityMetadata,
            Map<String, Object> rateLimitMetadata
    ) {
        apply(this.modelCode, modelName, status, enabled, defaultModel, capabilities, costMetadata, qualityMetadata, rateLimitMetadata);
    }

    public UUID getProviderId() {
        return providerId;
    }

    public String getModelCode() {
        return modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public ProviderStatus getStatus() {
        return status;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDefaultModel() {
        return defaultModel;
    }

    public List<String> getCapabilities() {
        return List.copyOf(capabilities);
    }

    public Map<String, Object> getCostMetadata() {
        return Map.copyOf(costMetadata);
    }

    public Map<String, Object> getQualityMetadata() {
        return Map.copyOf(qualityMetadata);
    }

    public Map<String, Object> getRateLimitMetadata() {
        return Map.copyOf(rateLimitMetadata);
    }

    private void apply(
            String modelCode,
            String modelName,
            ProviderStatus status,
            boolean enabled,
            boolean defaultModel,
            Collection<String> capabilities,
            Map<String, Object> costMetadata,
            Map<String, Object> qualityMetadata,
            Map<String, Object> rateLimitMetadata
    ) {
        this.modelCode = AiToolProvider.normalizeCode(modelCode, "modelCode");
        this.modelName = AiToolProvider.normalizeRequired(modelName, "modelName");
        this.status = status == null ? (enabled ? ProviderStatus.ACTIVE : ProviderStatus.DISABLED) : status;
        this.enabled = enabled;
        this.defaultModel = defaultModel;
        this.capabilities = AiToolProvider.normalizeStringList(capabilities);
        this.costMetadata = AiToolProvider.normalizeMetadata(costMetadata);
        this.qualityMetadata = AiToolProvider.normalizeMetadata(qualityMetadata);
        this.rateLimitMetadata = AiToolProvider.normalizeMetadata(rateLimitMetadata);
    }
}
