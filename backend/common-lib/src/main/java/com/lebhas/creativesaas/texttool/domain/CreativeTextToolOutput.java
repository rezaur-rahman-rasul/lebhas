package com.lebhas.creativesaas.texttool.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "creative_text_tool_outputs", schema = "platform")
public class CreativeTextToolOutput extends TenantAwareEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Column(name = "product_service_id")
    private UUID productServiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_type", nullable = false, length = 40)
    private CreativeTextToolType toolType;

    @Column(name = "tool_code", nullable = false, length = 120)
    private String toolCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_mode", nullable = false, length = 20)
    private CreativeTextQualityMode qualityMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 40)
    private PromptPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 20)
    private PromptLanguage language;

    @Column(name = "tone", length = 120)
    private String tone;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_objective", length = 40)
    private CampaignObjective campaignObjective;

    @Column(name = "source_idea", length = 2000)
    private String sourceIdea;

    @Column(name = "provider_id")
    private UUID providerId;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(name = "credit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditCost;

    @Column(name = "credit_reservation_id")
    private UUID creditReservationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_asset_ids", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> selectedAssetIds = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> outputPayload = new LinkedHashMap<>();

    protected CreativeTextToolOutput() {
    }

    public static CreativeTextToolOutput create(
            UUID workspaceId,
            UUID projectId,
            UUID brandId,
            UUID productServiceId,
            CreativeTextToolType toolType,
            String toolCode,
            CreativeTextQualityMode qualityMode,
            PromptPlatform platform,
            PromptLanguage language,
            String tone,
            CampaignObjective campaignObjective,
            String sourceIdea,
            UUID providerId,
            UUID modelId,
            BigDecimal creditCost,
            UUID creditReservationId,
            Map<String, Object> selectedAssetIds,
            Map<String, Object> outputPayload
    ) {
        CreativeTextToolOutput output = new CreativeTextToolOutput();
        output.assignWorkspace(require(workspaceId, "workspaceId"));
        output.projectId = require(projectId, "projectId");
        output.brandId = require(brandId, "brandId");
        output.productServiceId = productServiceId;
        output.toolType = require(toolType, "toolType");
        output.toolCode = normalizeRequired(toolCode, "toolCode");
        output.qualityMode = require(qualityMode, "qualityMode");
        output.platform = require(platform, "platform");
        output.language = require(language, "language");
        output.tone = normalizeNullable(tone);
        output.campaignObjective = campaignObjective;
        output.sourceIdea = normalizeNullable(sourceIdea);
        output.providerId = providerId;
        output.modelId = modelId;
        output.creditCost = require(creditCost, "creditCost");
        output.creditReservationId = creditReservationId;
        output.selectedAssetIds = safeMap(selectedAssetIds);
        output.outputPayload = safeMap(outputPayload);
        return output;
    }

    public UUID getProjectId() { return projectId; }
    public UUID getBrandId() { return brandId; }
    public UUID getProductServiceId() { return productServiceId; }
    public CreativeTextToolType getToolType() { return toolType; }
    public String getToolCode() { return toolCode; }
    public CreativeTextQualityMode getQualityMode() { return qualityMode; }
    public PromptPlatform getPlatform() { return platform; }
    public PromptLanguage getLanguage() { return language; }
    public String getTone() { return tone; }
    public CampaignObjective getCampaignObjective() { return campaignObjective; }
    public String getSourceIdea() { return sourceIdea; }
    public UUID getProviderId() { return providerId; }
    public UUID getModelId() { return modelId; }
    public BigDecimal getCreditCost() { return creditCost; }
    public UUID getCreditReservationId() { return creditReservationId; }
    public Map<String, Object> getSelectedAssetIds() { return Map.copyOf(selectedAssetIds); }
    public Map<String, Object> getOutputPayload() { return Map.copyOf(outputPayload); }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Map<String, Object> safeMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
}
