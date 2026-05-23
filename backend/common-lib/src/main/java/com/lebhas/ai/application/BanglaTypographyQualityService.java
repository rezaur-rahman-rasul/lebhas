package com.lebhas.ai.application;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BanglaTypographyQualityService {

    public BigDecimal score(BigDecimal measuredScore) {
        return QualityScoreCalculator.normalizeScore(measuredScore);
    }
}
