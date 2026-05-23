package com.lebhas.ai.application;

import com.lebhas.ai.domain.AiProviderMetrics;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ProviderReliabilityScorer {

    public BigDecimal reliabilityScore(AiProviderMetrics metrics) {
        if (metrics == null || metrics.getTotalRequests() <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(metrics.getSuccessfulRequests())
                .divide(BigDecimal.valueOf(metrics.getTotalRequests()), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal aggregateReliabilityScore(List<AiProviderMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        long totalRequests = metrics.stream().mapToLong(AiProviderMetrics::getTotalRequests).sum();
        if (totalRequests <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        long successfulRequests = metrics.stream().mapToLong(AiProviderMetrics::getSuccessfulRequests).sum();
        return BigDecimal.valueOf(successfulRequests)
                .divide(BigDecimal.valueOf(totalRequests), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal uptimePercentage(long successfulRequests, long totalRequests) {
        if (totalRequests <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(successfulRequests)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalRequests), 4, RoundingMode.HALF_UP);
    }
}
