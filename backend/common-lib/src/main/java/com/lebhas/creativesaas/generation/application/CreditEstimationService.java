package com.lebhas.creativesaas.generation.application;

import com.lebhas.ai.credit.application.CreditValuePolicyService;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationRequestEntity;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CreditEstimationService {

    private final ObjectProvider<CreditValuePolicyService> creditValuePolicyServiceProvider;

    public CreditEstimationService(ObjectProvider<CreditValuePolicyService> creditValuePolicyServiceProvider) {
        this.creditValuePolicyServiceProvider = creditValuePolicyServiceProvider;
    }

    public BigDecimal estimate(CreativeGenerationContext context) {
        return estimate(context == null ? null : context.creativeType());
    }

    public BigDecimal estimate(CreativeGenerationRequestEntity request) {
        return estimate(request == null ? null : request.getCreativeType());
    }

    public BigDecimal estimate(CreativeType creativeType) {
        return calculatePolicyCost(1);
    }

    public BigDecimal estimate(CreativeType creativeType, int requestedVersions) {
        return calculatePolicyCost(requestedVersions);
    }

    private BigDecimal calculatePolicyCost(int requestedVersions) {
        CreditValuePolicyService policyService = creditValuePolicyServiceProvider.getIfAvailable();
        if (policyService == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return policyService.calculateCreativeCreditCost(null, requestedVersions).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return amount.setScale(4, RoundingMode.HALF_UP);
    }
}
