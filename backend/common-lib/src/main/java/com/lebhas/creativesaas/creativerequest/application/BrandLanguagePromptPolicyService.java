package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import org.springframework.stereotype.Service;

@Service
public class BrandLanguagePromptPolicyService {

    public BrandLanguagePromptPolicy resolve(BrandLanguagePreference brandLanguagePreference, BrandLanguagePreference requestedLanguagePreference) {
        BrandLanguagePreference brandPreference = brandLanguagePreference == null
                ? BrandLanguagePreference.BOTH
                : brandLanguagePreference;
        BrandLanguagePreference effectiveRequestPreference = switch (brandPreference) {
            case BANGLA -> requireCompatible(brandPreference, requestedLanguagePreference, BrandLanguagePreference.BANGLA);
            case ENGLISH -> requireCompatible(brandPreference, requestedLanguagePreference, BrandLanguagePreference.ENGLISH);
            case BOTH -> requestedLanguagePreference == null ? BrandLanguagePreference.BOTH : requestedLanguagePreference;
        };
        return new BrandLanguagePromptPolicy(
                brandPreference,
                effectiveRequestPreference,
                toPromptLanguage(effectiveRequestPreference));
    }

    private BrandLanguagePreference requireCompatible(
            BrandLanguagePreference brandPreference,
            BrandLanguagePreference requestedLanguagePreference,
            BrandLanguagePreference enforcedPreference
    ) {
        if (requestedLanguagePreference == null) {
            return enforcedPreference;
        }
        if (requestedLanguagePreference != enforcedPreference) {
            throw new BusinessException(
                    ErrorCode.GENERATION_VALIDATION_FAILED,
                    "Selected language must follow the brand language preference: " + brandPreference.name());
        }
        return requestedLanguagePreference;
    }

    private PromptLanguage toPromptLanguage(BrandLanguagePreference preference) {
        return switch (preference) {
            case BANGLA -> PromptLanguage.BANGLA;
            case ENGLISH -> PromptLanguage.ENGLISH;
            case BOTH -> PromptLanguage.MIXED;
        };
    }

    public record BrandLanguagePromptPolicy(
            BrandLanguagePreference brandLanguagePreference,
            BrandLanguagePreference requestLanguagePreference,
            PromptLanguage promptLanguage
    ) {
    }
}
