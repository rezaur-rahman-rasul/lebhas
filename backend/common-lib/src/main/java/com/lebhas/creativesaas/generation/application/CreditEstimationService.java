package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.generation.domain.CreativeGenerationRequestEntity;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CreditEstimationService {

    private static final BigDecimal IMAGE_GENERATION_COST = new BigDecimal("1.0000");
    private static final BigDecimal VIDEO_GENERATION_COST = new BigDecimal("3.0000");

    public BigDecimal estimate(CreativeGenerationContext context) {
        return estimate(context == null ? null : context.creativeType());
    }

    public BigDecimal estimate(CreativeGenerationRequestEntity request) {
        return estimate(request == null ? null : request.getCreativeType());
    }

    public BigDecimal estimate(CreativeType creativeType) {
        if (creativeType == null) {
            return IMAGE_GENERATION_COST;
        }
        return creativeType.isVideo() ? VIDEO_GENERATION_COST : IMAGE_GENERATION_COST;
    }

    public BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return amount.setScale(4, RoundingMode.HALF_UP);
    }
}
