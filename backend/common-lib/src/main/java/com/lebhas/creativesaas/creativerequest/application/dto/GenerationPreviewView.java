package com.lebhas.creativesaas.creativerequest.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record GenerationPreviewView(
        UUID creativeRequestId,
        int requestedVersionCount,
        BigDecimal estimatedCreditCost,
        boolean creditsReserved
) {
}
