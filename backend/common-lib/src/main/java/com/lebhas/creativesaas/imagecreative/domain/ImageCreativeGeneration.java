package com.lebhas.creativesaas.imagecreative.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "image_creative_generations", schema = "platform")
public class ImageCreativeGeneration extends TenantAwareEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "creative_request_id")
    private UUID creativeRequestId;

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Column(name = "product_service_id")
    private UUID productServiceId;

    @Column(name = "product_asset_id")
    private UUID productAssetId;

    @Column(name = "tool_code", nullable = false, length = 120)
    private String toolCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "creative_format", nullable = false, length = 40)
    private ImageCreativeFormat creativeFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 40)
    private PromptPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 20)
    private PromptLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_mode", nullable = false, length = 20)
    private ImageCreativeQualityMode qualityMode;

    @Column(name = "requested_version_count", nullable = false)
    private int requestedVersionCount;

    @Column(name = "credit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditCost;

    @Column(name = "credit_reservation_id")
    private UUID creditReservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ImageCreativeGenerationStatus status;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_version_ids", nullable = false, columnDefinition = "jsonb")
    private List<UUID> generatedVersionIds = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> requestPayload = new LinkedHashMap<>();

    protected ImageCreativeGeneration() {
    }

    public static ImageCreativeGeneration requested(
            UUID workspaceId,
            UUID projectId,
            UUID brandId,
            UUID productServiceId,
            UUID productAssetId,
            String toolCode,
            ImageCreativeFormat creativeFormat,
            PromptPlatform platform,
            PromptLanguage language,
            ImageCreativeQualityMode qualityMode,
            int requestedVersionCount,
            BigDecimal creditCost,
            Map<String, Object> requestPayload
    ) {
        ImageCreativeGeneration generation = new ImageCreativeGeneration();
        generation.assignWorkspace(require(workspaceId, "workspaceId"));
        generation.projectId = require(projectId, "projectId");
        generation.brandId = require(brandId, "brandId");
        generation.productServiceId = productServiceId;
        generation.productAssetId = productAssetId;
        generation.toolCode = requireText(toolCode, "toolCode");
        generation.creativeFormat = require(creativeFormat, "creativeFormat");
        generation.platform = require(platform, "platform");
        generation.language = require(language, "language");
        generation.qualityMode = require(qualityMode, "qualityMode");
        generation.requestedVersionCount = requestedVersionCount;
        generation.creditCost = require(creditCost, "creditCost");
        generation.status = ImageCreativeGenerationStatus.REQUESTED;
        generation.requestPayload = requestPayload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(requestPayload);
        return generation;
    }

    public void attachCreativeRequest(UUID creativeRequestId) {
        this.creativeRequestId = creativeRequestId;
    }

    public void attachCreditReservation(UUID creditReservationId) {
        this.creditReservationId = creditReservationId;
    }

    public void complete(List<UUID> generatedVersionIds) {
        this.generatedVersionIds = generatedVersionIds == null ? List.of() : List.copyOf(generatedVersionIds);
        this.status = ImageCreativeGenerationStatus.COMPLETED;
        this.failureReason = null;
    }

    public void fail(String failureReason) {
        this.status = ImageCreativeGenerationStatus.FAILED;
        this.failureReason = failureReason == null ? null : failureReason.replaceAll("\\s+", " ").trim();
    }

    public UUID getProjectId() { return projectId; }
    public UUID getCreativeRequestId() { return creativeRequestId; }
    public UUID getBrandId() { return brandId; }
    public UUID getProductServiceId() { return productServiceId; }
    public UUID getProductAssetId() { return productAssetId; }
    public String getToolCode() { return toolCode; }
    public ImageCreativeFormat getCreativeFormat() { return creativeFormat; }
    public PromptPlatform getPlatform() { return platform; }
    public PromptLanguage getLanguage() { return language; }
    public ImageCreativeQualityMode getQualityMode() { return qualityMode; }
    public int getRequestedVersionCount() { return requestedVersionCount; }
    public BigDecimal getCreditCost() { return creditCost; }
    public UUID getCreditReservationId() { return creditReservationId; }
    public ImageCreativeGenerationStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public List<UUID> getGeneratedVersionIds() { return List.copyOf(generatedVersionIds); }
    public Map<String, Object> getRequestPayload() { return Map.copyOf(requestPayload); }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
