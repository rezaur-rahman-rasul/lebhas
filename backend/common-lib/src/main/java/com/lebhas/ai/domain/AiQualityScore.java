package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ai_quality_scores", schema = "platform")
public class AiQualityScore extends BaseEntity {

    @Column(name = "generated_version_id", nullable = false)
    private UUID generatedVersionId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "overall_score", nullable = false, precision = 8, scale = 4)
    private BigDecimal overallScore = BigDecimal.ZERO;

    @Column(name = "text_readability_score", nullable = false, precision = 8, scale = 4)
    private BigDecimal textReadabilityScore = BigDecimal.ZERO;

    @Column(name = "product_preservation_score", nullable = false, precision = 8, scale = 4)
    private BigDecimal productPreservationScore = BigDecimal.ZERO;

    @Column(name = "branding_score", nullable = false, precision = 8, scale = 4)
    private BigDecimal brandingScore = BigDecimal.ZERO;

    @Column(name = "bangla_typography_score", nullable = false, precision = 8, scale = 4)
    private BigDecimal banglaTypographyScore = BigDecimal.ZERO;

    @Column(name = "composition_score", nullable = false, precision = 8, scale = 4)
    private BigDecimal compositionScore = BigDecimal.ZERO;

    @Column(name = "quality_notes", columnDefinition = "TEXT")
    private String qualityNotes;

    protected AiQualityScore() {
    }

    public static AiQualityScore create(
            UUID generatedVersionId,
            UUID workspaceId,
            BigDecimal overallScore,
            BigDecimal textReadabilityScore,
            BigDecimal productPreservationScore,
            BigDecimal brandingScore,
            BigDecimal banglaTypographyScore,
            BigDecimal compositionScore,
            String qualityNotes
    ) {
        AiQualityScore score = new AiQualityScore();
        score.generatedVersionId = AiToolProvider.require(generatedVersionId, "generatedVersionId");
        score.workspaceId = AiToolProvider.require(workspaceId, "workspaceId");
        score.overallScore = AiProviderMetrics.nonNegative(overallScore);
        score.textReadabilityScore = AiProviderMetrics.nonNegative(textReadabilityScore);
        score.productPreservationScore = AiProviderMetrics.nonNegative(productPreservationScore);
        score.brandingScore = AiProviderMetrics.nonNegative(brandingScore);
        score.banglaTypographyScore = AiProviderMetrics.nonNegative(banglaTypographyScore);
        score.compositionScore = AiProviderMetrics.nonNegative(compositionScore);
        score.qualityNotes = AiToolProvider.normalizeNullable(qualityNotes);
        return score;
    }

    public void updateScores(
            BigDecimal overallScore,
            BigDecimal textReadabilityScore,
            BigDecimal productPreservationScore,
            BigDecimal brandingScore,
            BigDecimal banglaTypographyScore,
            BigDecimal compositionScore,
            String qualityNotes
    ) {
        this.overallScore = AiProviderMetrics.nonNegative(overallScore);
        this.textReadabilityScore = AiProviderMetrics.nonNegative(textReadabilityScore);
        this.productPreservationScore = AiProviderMetrics.nonNegative(productPreservationScore);
        this.brandingScore = AiProviderMetrics.nonNegative(brandingScore);
        this.banglaTypographyScore = AiProviderMetrics.nonNegative(banglaTypographyScore);
        this.compositionScore = AiProviderMetrics.nonNegative(compositionScore);
        this.qualityNotes = AiToolProvider.normalizeNullable(qualityNotes);
    }

    public UUID getGeneratedVersionId() {
        return generatedVersionId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public BigDecimal getTextReadabilityScore() {
        return textReadabilityScore;
    }

    public BigDecimal getProductPreservationScore() {
        return productPreservationScore;
    }

    public BigDecimal getBrandingScore() {
        return brandingScore;
    }

    public BigDecimal getBanglaTypographyScore() {
        return banglaTypographyScore;
    }

    public BigDecimal getCompositionScore() {
        return compositionScore;
    }

    public String getQualityNotes() {
        return qualityNotes;
    }
}
