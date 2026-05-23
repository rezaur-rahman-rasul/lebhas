package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CalculatedQualityScore;
import com.lebhas.ai.application.dto.QualityScoreInput;
import com.lebhas.ai.application.dto.QualityScoreResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AiQualityScoringService {

    private final QualityScoreCalculator qualityScoreCalculator;
    private final GeneratedVersionQualityService generatedVersionQualityService;

    public AiQualityScoringService(
            QualityScoreCalculator qualityScoreCalculator,
            GeneratedVersionQualityService generatedVersionQualityService
    ) {
        this.qualityScoreCalculator = qualityScoreCalculator;
        this.generatedVersionQualityService = generatedVersionQualityService;
    }

    @Transactional(readOnly = true)
    public CalculatedQualityScore calculate(QualityScoreInput input) {
        return qualityScoreCalculator.calculate(input);
    }

    @Transactional
    public QualityScoreResult scoreGeneratedVersion(QualityScoreInput input) {
        return generatedVersionQualityService.scoreGeneratedVersion(input);
    }

    @Transactional(readOnly = true)
    public QualityScoreResult getGeneratedVersionQuality(UUID generatedVersionId) {
        return generatedVersionQualityService.getQualityScore(generatedVersionId);
    }
}
