package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.LayerToolMapping;

import java.math.BigDecimal;

public record LayerToolCandidate(
        LayerToolMapping mapping,
        AiToolProvider provider,
        BigDecimal estimatedCost,
        BigDecimal qualityScore
) {
}
