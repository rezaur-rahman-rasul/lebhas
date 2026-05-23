package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.domain.BrandStatus;
import com.lebhas.creativesaas.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBrandRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 120)
        String name,
        @Size(max = 80)
        String businessType,
        @Size(max = 80)
        String industry,
        @Size(max = 160)
        String targetAudience,
        @Size(max = 120)
        String brandVoice,
        @Size(max = 120)
        String preferredCta,
        @Size(max = 7)
        String primaryColor,
        @Size(max = 7)
        String secondaryColor,
        @Size(max = 300)
        String website,
        @Size(max = 300)
        String facebookUrl,
        @Size(max = 300)
        String instagramUrl,
        @Size(max = 300)
        String linkedinUrl,
        @Size(max = 300)
        String tiktokUrl,
        @NotNull(message = ValidationMessages.REQUIRED)
        BrandLanguagePreference languagePreference,
        BrandStatus status
) {
}
