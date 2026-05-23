package com.lebhas.creativesaas.sharing.cache;

import com.lebhas.creativesaas.sharing.domain.ShareLink;

import java.time.Instant;
import java.util.UUID;

public record ShareLinkCacheEntry(
        UUID shareLinkId,
        UUID workspaceId,
        UUID generatedVersionId,
        String token,
        Instant expiresAt,
        long accessCount,
        UUID createdBy,
        Instant createdAt
) {

    public static ShareLinkCacheEntry from(ShareLink shareLink) {
        return new ShareLinkCacheEntry(
                shareLink.getId(),
                shareLink.getWorkspaceId(),
                shareLink.getGeneratedVersionId(),
                shareLink.getToken(),
                shareLink.getExpiresAt(),
                shareLink.getAccessCount(),
                shareLink.getCreatedBy(),
                shareLink.getCreatedAt());
    }
}
