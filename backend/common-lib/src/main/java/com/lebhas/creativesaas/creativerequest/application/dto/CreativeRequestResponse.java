package com.lebhas.creativesaas.creativerequest.application.dto;

import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;

import java.math.BigDecimal;
import java.util.List;

public record CreativeRequestResponse(
        CreativeRequestView request,
        GeneratedVersionView latestVersion,
        List<GeneratedVersionView> versions,
        CreativeRequestJobView job,
        BigDecimal estimatedCreditCost
) {
}
