package com.lebhas.creativesaas.brand.application.dto;

import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.domain.BrandStatus;

import java.time.Instant;
import java.util.UUID;

public record BrandView(
        UUID id,
        UUID workspaceId,
        UUID ownerUserId,
        String name,
        String businessType,
        String industry,
        String targetAudience,
        String brandVoice,
        String preferredCta,
        String primaryColor,
        String secondaryColor,
        String website,
        String facebookUrl,
        String instagramUrl,
        String linkedinUrl,
        String tiktokUrl,
        BrandLanguagePreference languagePreference,
        BrandStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
