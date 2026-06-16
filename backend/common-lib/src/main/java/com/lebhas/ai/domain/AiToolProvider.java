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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ai_tool_providers", schema = "platform")
public class AiToolProvider extends BaseEntity {

    @Column(name = "provider_code", nullable = false, length = 80)
    private String providerCode;

    @Column(name = "provider_name", nullable = false, length = 160)
    private String providerName;

    @Column(name = "description")
    private String description;

    @Column(name = "category", length = 80)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 40)
    private ProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProviderStatus status;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "supports_sandbox", nullable = false)
    private boolean supportsSandbox = true;

    @Column(name = "supports_live", nullable = false)
    private boolean supportsLive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_environment", nullable = false, length = 20)
    private ProviderEnvironment defaultEnvironment = ProviderEnvironment.SANDBOX;

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

    @Column(name = "openai_admin_api_key_encrypted")
    private String openAiAdminApiKeyEncrypted;

    @Column(name = "provider_top_up_amount_usd", precision = 19, scale = 4)
    private BigDecimal providerTopUpAmountUsd;

    @Column(name = "provider_top_up_date")
    private LocalDate providerTopUpDate;

    @Column(name = "provider_manual_balance_usd", precision = 19, scale = 4)
    private BigDecimal providerManualBalanceUsd;

    @Column(name = "last_cost_sync_at")
    private Instant lastCostSyncAt;

    @Column(name = "total_cost_spent_usd", precision = 19, scale = 4)
    private BigDecimal totalCostSpentUsd;

    @Column(name = "estimated_remaining_balance_usd", precision = 19, scale = 4)
    private BigDecimal estimatedRemainingBalanceUsd;

    @Column(name = "cost_sync_enabled", nullable = false)
    private boolean costSyncEnabled;

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

    public void updateSettings(
            String providerName,
            String description,
            ProviderStatus status,
            boolean supportsSandbox,
            boolean supportsLive,
            ProviderEnvironment defaultEnvironment
    ) {
        this.providerName = normalizeRequired(providerName, "displayName");
        this.description = normalizeNullable(description);
        this.category = "MASTER_CONFIGURED";
        this.status = status == null ? ProviderStatus.ACTIVE : status;
        this.enabled = this.status == ProviderStatus.ACTIVE;
        this.supportsSandbox = supportsSandbox;
        this.supportsLive = supportsLive;
        this.defaultEnvironment = defaultEnvironment == null ? ProviderEnvironment.SANDBOX : defaultEnvironment;
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

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public ProviderStatus getStatus() {
        return status;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSupportsSandbox() {
        return supportsSandbox;
    }

    public boolean isSupportsLive() {
        return supportsLive;
    }

    public ProviderEnvironment getDefaultEnvironment() {
        return defaultEnvironment;
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

    public String getOpenAiAdminApiKeyEncrypted() {
        return openAiAdminApiKeyEncrypted;
    }

    public BigDecimal getProviderTopUpAmountUsd() {
        return providerTopUpAmountUsd;
    }

    public LocalDate getProviderTopUpDate() {
        return providerTopUpDate;
    }

    public BigDecimal getProviderManualBalanceUsd() {
        return providerManualBalanceUsd;
    }

    public Instant getLastCostSyncAt() {
        return lastCostSyncAt;
    }

    public BigDecimal getTotalCostSpentUsd() {
        return totalCostSpentUsd;
    }

    public BigDecimal getEstimatedRemainingBalanceUsd() {
        return estimatedRemainingBalanceUsd;
    }

    public boolean isCostSyncEnabled() {
        return costSyncEnabled;
    }

    public void configureOpenAiCostTracking(
            String openAiAdminApiKeyEncrypted,
            BigDecimal providerTopUpAmountUsd,
            LocalDate providerTopUpDate,
            BigDecimal providerManualBalanceUsd,
            Boolean costSyncEnabled
    ) {
        this.openAiAdminApiKeyEncrypted = normalizeNullable(openAiAdminApiKeyEncrypted);
        this.providerTopUpAmountUsd = nonNegative(providerTopUpAmountUsd);
        this.providerTopUpDate = providerTopUpDate;
        this.providerManualBalanceUsd = nonNegative(providerManualBalanceUsd);
        this.costSyncEnabled = costSyncEnabled != null && costSyncEnabled;
        recalculateEstimatedRemainingBalance();
    }

    public void applyOpenAiCostSync(BigDecimal totalCostSpentUsd, BigDecimal estimatedRemainingBalanceUsd, Instant syncedAt) {
        this.totalCostSpentUsd = nonNegative(totalCostSpentUsd);
        this.estimatedRemainingBalanceUsd = nonNegative(estimatedRemainingBalanceUsd);
        this.lastCostSyncAt = require(syncedAt, "syncedAt");
    }

    public void recordOpenAiSpend(BigDecimal amountUsd, Instant recordedAt) {
        BigDecimal amount = nonNegative(amountUsd);
        BigDecimal currentSpend = totalCostSpentUsd == null ? BigDecimal.ZERO : totalCostSpentUsd;
        this.totalCostSpentUsd = currentSpend.add(amount);
        this.lastCostSyncAt = require(recordedAt, "recordedAt");
        recalculateEstimatedRemainingBalance();
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

    public static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public static String normalizeNullable(String value) {
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

    private void recalculateEstimatedRemainingBalance() {
        if (providerManualBalanceUsd != null) {
            estimatedRemainingBalanceUsd = providerManualBalanceUsd;
            return;
        }
        if (providerTopUpAmountUsd == null) {
            estimatedRemainingBalanceUsd = null;
            return;
        }
        BigDecimal spent = totalCostSpentUsd == null ? BigDecimal.ZERO : totalCostSpentUsd;
        BigDecimal remaining = providerTopUpAmountUsd.subtract(spent);
        estimatedRemainingBalanceUsd = remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("OpenAI cost tracking values must not be negative");
        }
        return value;
    }
}
