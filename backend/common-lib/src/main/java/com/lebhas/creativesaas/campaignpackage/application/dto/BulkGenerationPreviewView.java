package com.lebhas.creativesaas.campaignpackage.application.dto;

import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationType;

import java.math.BigDecimal;

public record BulkGenerationPreviewView(
        BulkGenerationType generationType,
        int itemCount,
        BigDecimal unitCreditCost,
        BigDecimal estimatedCredits
) {
}
