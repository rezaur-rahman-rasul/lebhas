package com.lebhas.creativesaas.sharing.cache;

import com.lebhas.creativesaas.sharing.domain.ShareLink;

import java.time.Instant;
import java.util.UUID;

public record ShareLinkCacheEntry(
        UUID shareLinkId,
        UUID workspaceId,
        UUID generatedVersionId,
        String tokenHash,
        Instant expiresAt,
        long accessCount,
        boolean revoked,
        Instant revokedAt,
        UUID createdBy,
        Instant createdAt
) {

    public static ShareLinkCacheEntry from(ShareLink shareLink) {
        return new ShareLinkCacheEntry(
                shareLink.getId(),
                shareLink.getWorkspaceId(),
                shareLink.getGeneratedVersionId(),
                shareLink.getTokenHash(),
                shareLink.getExpiresAt(),
                shareLink.getAccessCount(),
                shareLink.isRevoked(),
                shareLink.getRevokedAt(),
                shareLink.getCreatedBy(),
                shareLink.getCreatedAt());
    }
}
