package com.lebhas.creativesaas.workspace.application.dto;

import java.util.UUID;

public record UpdateBrandProfileCommand(
        UUID workspaceId,
        String brandName,
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
        String description
) {
}
