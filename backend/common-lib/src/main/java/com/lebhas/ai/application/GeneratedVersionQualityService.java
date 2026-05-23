package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CalculatedQualityScore;
import com.lebhas.ai.application.dto.CostObservation;
import com.lebhas.ai.application.dto.QualityScoreInput;
import com.lebhas.ai.application.dto.QualityScoreResult;
import com.lebhas.ai.domain.AiQualityScore;
import com.lebhas.ai.infrastructure.persistence.AiQualityScoreRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class GeneratedVersionQualityService {

    private final GeneratedVersionRepository generatedVersionRepository;
    private final AiQualityScoreRepository qualityScoreRepository;
    private final QualityScoreCalculator qualityScoreCalculator;
    private final AiCostIntelligenceService aiCostIntelligenceService;

    public GeneratedVersionQualityService(
            GeneratedVersionRepository generatedVersionRepository,
            AiQualityScoreRepository qualityScoreRepository,
            QualityScoreCalculator qualityScoreCalculator,
            AiCostIntelligenceService aiCostIntelligenceService
    ) {
        this.generatedVersionRepository = generatedVersionRepository;
        this.qualityScoreRepository = qualityScoreRepository;
        this.qualityScoreCalculator = qualityScoreCalculator;
        this.aiCostIntelligenceService = aiCostIntelligenceService;
    }

    @Transactional
    public QualityScoreResult scoreGeneratedVersion(QualityScoreInput input) {
        GeneratedVersionEntity version = requireGeneratedVersion(input.workspaceId(), input.generatedVersionId());
        CalculatedQualityScore calculated = qualityScoreCalculator.calculate(input);
        AiQualityScore qualityScore = qualityScoreRepository
                .findByGeneratedVersionIdAndDeletedFalse(version.getId())
                .map(existing -> {
                    existing.updateScores(
                            calculated.overallScore(),
                            calculated.textReadabilityScore(),
                            calculated.productPreservationScore(),
                            calculated.brandingScore(),
                            calculated.banglaTypographyScore(),
                            calculated.compositionScore(),
                            calculated.qualityNotes());
                    return existing;
                })
                .orElseGet(() -> AiQualityScore.create(
                        version.getId(),
                        version.getWorkspaceId(),
                        calculated.overallScore(),
                        calculated.textReadabilityScore(),
                        calculated.productPreservationScore(),
                        calculated.brandingScore(),
                        calculated.banglaTypographyScore(),
                        calculated.compositionScore(),
                        calculated.qualityNotes()));
        AiQualityScore saved = qualityScoreRepository.save(qualityScore);
        trackAnalyticsIfPresent(input, calculated);
        return toResult(saved);
    }

    @Transactional(readOnly = true)
    public QualityScoreResult getQualityScore(UUID generatedVersionId) {
        AiQualityScore score = qualityScoreRepository.findByGeneratedVersionIdAndDeletedFalse(generatedVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI quality score not found"));
        return toResult(score);
    }

    private GeneratedVersionEntity requireGeneratedVersion(UUID workspaceId, UUID generatedVersionId) {
        if (workspaceId == null || generatedVersionId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "workspaceId and generatedVersionId are required");
        }
        return generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATED_VERSION_NOT_FOUND));
    }

    private void trackAnalyticsIfPresent(QualityScoreInput input, CalculatedQualityScore calculated) {
        if (input.providerId() == null || input.modelName() == null || input.modelName().isBlank()) {
            return;
        }
        CostObservation observation = new CostObservation(
                input.providerId(),
                input.layerId(),
                input.modelName(),
                input.costUsd(),
                calculated.overallScore(),
                input.latencyMs(),
                input.successful() == null || input.successful(),
                Instant.now());
        aiCostIntelligenceService.trackProviderCost(observation);
        if (input.layerId() != null) {
            aiCostIntelligenceService.trackLayerCost(observation);
        }
    }

    private QualityScoreResult toResult(AiQualityScore score) {
        return new QualityScoreResult(
                score.getId(),
                score.getWorkspaceId(),
                score.getGeneratedVersionId(),
                score.getOverallScore(),
                score.getTextReadabilityScore(),
                score.getProductPreservationScore(),
                score.getBrandingScore(),
                score.getBanglaTypographyScore(),
                score.getCompositionScore(),
                score.getQualityNotes());
    }
}
