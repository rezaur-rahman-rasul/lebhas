package com.lebhas.creativesaas.imagecreative.application.dto;

import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeQualityMode;

import java.math.BigDecimal;

public record ImageCreativeCostPreviewView(
        String toolCode,
        ImageCreativeQualityMode qualityMode,
        int requestedVersionCount,
        BigDecimal unitCreditCost,
        BigDecimal totalCreditCost
) {
}
