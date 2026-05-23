package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.ProviderCostOption;
import com.lebhas.ai.domain.AiModel;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.LayerToolMapping;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CostEfficiencyCalculator {

    private static final List<String> FIXED_COST_KEYS = List.of(
            "estimatedCostUsd",
            "costUsd",
            "costPerRunUsd",
            "costPerRun",
            "baseCostUsd",
            "baseCost");
    private static final List<String> UNIT_COST_KEYS = List.of(
            "unitCostUsd",
            "unitCost",
            "costPerUnitUsd",
            "costPerUnit");
    private static final List<String> QUALITY_KEYS = List.of(
            "estimatedQualityScore",
            "qualityScore",
            "avgQualityScore",
            "score");

    public ProviderCostOption toOption(
            UUID layerId,
            LayerToolMapping mapping,
            AiToolProvider provider,
            AiModel model,
            BigDecimal requestedUnits,
            boolean eligible,
            String ineligibilityReason
    ) {
        Map<String, Object> costMetadata = mergedMetadata(provider == null ? Map.of() : provider.getCostMetadata(),
                model == null ? Map.of() : model.getCostMetadata());
        Map<String, Object> qualityMetadata = mergedMetadata(provider == null ? Map.of() : provider.getQualityMetadata(),
                model == null ? Map.of() : model.getQualityMetadata());
        BigDecimal estimatedCost = estimateCostUsd(costMetadata, requestedUnits);
        BigDecimal qualityScore = estimateQualityScore(qualityMetadata);
        BigDecimal ratio = qualityToCostRatio(qualityScore, estimatedCost);
        return new ProviderCostOption(
                layerId,
                mapping == null ? null : mapping.getId(),
                provider == null ? null : provider.getId(),
                provider == null ? null : provider.getProviderCode(),
                provider == null ? null : provider.getProviderName(),
                model == null ? null : model.getId(),
                model == null ? null : model.getModelCode(),
                model == null ? null : model.getModelName(),
                estimatedCost,
                qualityScore,
                ratio,
                estimatedCost != null,
                qualityScore != null,
                eligible,
                ineligibilityReason);
    }

    public BigDecimal estimateCostUsd(Map<String, Object> metadata, BigDecimal requestedUnits) {
        Map<String, Object> normalized = metadata == null ? Map.of() : metadata;
        BigDecimal fixedCost = firstDecimal(normalized, FIXED_COST_KEYS);
        if (fixedCost != null) {
            return money(fixedCost);
        }
        BigDecimal unitCost = firstDecimal(normalized, UNIT_COST_KEYS);
        if (unitCost == null) {
            return null;
        }
        BigDecimal units = requestedUnits == null || requestedUnits.signum() <= 0 ? BigDecimal.ONE : requestedUnits;
        return money(unitCost.multiply(units));
    }

    public BigDecimal estimateQualityScore(Map<String, Object> metadata) {
        BigDecimal score = firstDecimal(metadata == null ? Map.of() : metadata, QUALITY_KEYS);
        if (score == null) {
            return null;
        }
        if (score.compareTo(BigDecimal.ONE) > 0 && score.compareTo(BigDecimal.valueOf(100)) <= 0) {
            score = score.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        }
        if (score.signum() < 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (score.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        return score.setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal qualityToCostRatio(BigDecimal qualityScore, BigDecimal costUsd) {
        if (qualityScore == null || costUsd == null || costUsd.signum() <= 0) {
            return null;
        }
        return qualityScore.divide(costUsd, 6, RoundingMode.HALF_UP);
    }

    public boolean isLowerCost(ProviderCostOption candidate, ProviderCostOption current) {
        if (candidate == null || current == null || !candidate.costKnown() || !current.costKnown()) {
            return false;
        }
        return candidate.estimatedCostUsd().compareTo(current.estimatedCostUsd()) < 0;
    }

    private Map<String, Object> mergedMetadata(Map<String, Object> providerMetadata, Map<String, Object> modelMetadata) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (providerMetadata != null) {
            merged.putAll(providerMetadata);
        }
        if (modelMetadata != null) {
            merged.putAll(modelMetadata);
        }
        return merged;
    }

    private BigDecimal firstDecimal(Map<String, Object> metadata, List<String> keys) {
        for (String key : keys) {
            BigDecimal value = decimal(metadata.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            String normalized = value.toString().trim();
            return normalized.isEmpty() ? null : new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.signum() < 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }
}
