package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Entity
@Table(name = "ai_tool_providers", schema = "platform")
public class AiToolProvider extends BaseEntity {

    @Column(name = "provider_code", nullable = false, length = 80)
    private String providerCode;

    @Column(name = "provider_name", nullable = false, length = 160)
    private String providerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 40)
    private ProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProviderStatus status;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supported_layers", nullable = false, columnDefinition = "jsonb")
    private List<String> supportedLayers = new ArrayList<>();

    @Column(name = "credential_config_key", length = 160)
    private String credentialConfigKey;

    @Column(name = "fallback_eligible", nullable = false)
    private boolean fallbackEligible;

    @Column(name = "workspace_routing_eligible", nullable = false)
    private boolean workspaceRoutingEligible;

    @Column(name = "plan_routing_eligible", nullable = false)
    private boolean planRoutingEligible;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cost_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> costMetadata = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quality_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> qualityMetadata = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rate_limit_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rateLimitMetadata = new LinkedHashMap<>();

    protected AiToolProvider() {
    }

    public static AiToolProvider create(
            String providerCode,
            String providerName,
            ProviderType providerType,
            ProviderStatus status,
            boolean enabled,
            Collection<String> supportedLayers,
            String credentialConfigKey,
            boolean fallbackEligible,
            boolean workspaceRoutingEligible,
            boolean planRoutingEligible,
            Map<String, Object> costMetadata,
            Map<String, Object> qualityMetadata,
            Map<String, Object> rateLimitMetadata
    ) {
        AiToolProvider provider = new AiToolProvider();
        provider.apply(providerCode, providerName, providerType, status, enabled, supportedLayers, credentialConfigKey,
                fallbackEligible, workspaceRoutingEligible, planRoutingEligible, costMetadata, qualityMetadata, rateLimitMetadata);
        return provider;
    }

    public void update(
            String providerName,
            ProviderType providerType,
            ProviderStatus status,
            boolean enabled,
            Collection<String> supportedLayers,
            String credentialConfigKey,
            boolean fallbackEligible,
            boolean workspaceRoutingEligible,
            boolean planRoutingEligible,
            Map<String, Object> costMetadata,
            Map<String, Object> qualityMetadata,
            Map<String, Object> rateLimitMetadata
    ) {
        apply(this.providerCode, providerName, providerType, status, enabled, supportedLayers, credentialConfigKey,
                fallbackEligible, workspaceRoutingEligible, planRoutingEligible, costMetadata, qualityMetadata, rateLimitMetadata);
    }

    public void enable() {
        this.enabled = true;
        this.status = ProviderStatus.ACTIVE;
    }

    public void disable() {
        this.enabled = false;
        this.status = ProviderStatus.DISABLED;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getProviderName() {
        return providerName;
    }

    public ProviderType getProviderType() {
        return providerType;
    }

    public ProviderStatus getStatus() {
        return status;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getSupportedLayers() {
        return List.copyOf(supportedLayers);
    }

    public String getCredentialConfigKey() {
        return credentialConfigKey;
    }

    public boolean isFallbackEligible() {
        return fallbackEligible;
    }

    public boolean isWorkspaceRoutingEligible() {
        return workspaceRoutingEligible;
    }

    public boolean isPlanRoutingEligible() {
        return planRoutingEligible;
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
            String providerCode,
            String providerName,
            ProviderType providerType,
            ProviderStatus status,
            boolean enabled,
            Collection<String> supportedLayers,
            String credentialConfigKey,
            boolean fallbackEligible,
            boolean workspaceRoutingEligible,
            boolean planRoutingEligible,
            Map<String, Object> costMetadata,
            Map<String, Object> qualityMetadata,
            Map<String, Object> rateLimitMetadata
    ) {
        this.providerCode = normalizeCode(providerCode, "providerCode");
        this.providerName = normalizeRequired(providerName, "providerName");
        this.providerType = require(providerType, "providerType");
        this.status = status == null ? (enabled ? ProviderStatus.ACTIVE : ProviderStatus.DISABLED) : status;
        this.enabled = enabled;
        this.supportedLayers = normalizeStringList(supportedLayers);
        this.credentialConfigKey = normalizeNullable(credentialConfigKey);
        this.fallbackEligible = fallbackEligible;
        this.workspaceRoutingEligible = workspaceRoutingEligible;
        this.planRoutingEligible = planRoutingEligible;
        this.costMetadata = normalizeMetadata(costMetadata);
        this.qualityMetadata = normalizeMetadata(qualityMetadata);
        this.rateLimitMetadata = normalizeMetadata(rateLimitMetadata);
    }

    public static String normalizeCode(String value, String field) {
        String normalized = normalizeRequired(value, field)
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z0-9_]{2,80}$")) {
            throw new IllegalArgumentException(field + " must contain only letters, numbers, or underscores");
        }
        return normalized;
    }

    static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    static List<String> normalizeStringList(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String current = normalizeNullable(value);
            if (current != null) {
                normalized.add(current.toUpperCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(normalized);
    }

    static Map<String, Object> normalizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(metadata);
    }
}
