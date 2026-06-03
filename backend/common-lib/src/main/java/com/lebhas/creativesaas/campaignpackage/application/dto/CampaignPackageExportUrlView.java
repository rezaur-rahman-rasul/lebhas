package com.lebhas.creativesaas.campaignpackage.application.dto;

import java.time.Instant;
import java.util.UUID;

public record CampaignPackageExportUrlView(
        UUID packageId,
        String r2ObjectKey,
        String signedUrl,
        Instant expiresAt
) {
}
