package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import com.lebhas.creativesaas.common.validation.color.HexColor;
import com.lebhas.creativesaas.common.validation.url.OptionalHttpUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBrandProfileRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 120)
        String brandName,
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
        @HexColor(message = ValidationMessages.INVALID_COLOR)
        String primaryColor,
        @HexColor(message = ValidationMessages.INVALID_COLOR)
        String secondaryColor,
        @Size(max = 300)
        @OptionalHttpUrl(message = ValidationMessages.INVALID_URL)
        String website,
        @Size(max = 300)
        @OptionalHttpUrl(message = ValidationMessages.INVALID_URL)
        String facebookUrl,
        @Size(max = 300)
        @OptionalHttpUrl(message = ValidationMessages.INVALID_URL)
        String instagramUrl,
        @Size(max = 300)
        @OptionalHttpUrl(message = ValidationMessages.INVALID_URL)
        String linkedinUrl,
        @Size(max = 300)
        @OptionalHttpUrl(message = ValidationMessages.INVALID_URL)
        String tiktokUrl,
        @Size(max = 1000)
        String description
) {
}
