package com.lebhas.creativesaas.sharing.application;

import com.lebhas.creativesaas.sharing.application.dto.RevisedShareLinkView;
import com.lebhas.creativesaas.sharing.cache.ShareLinkCacheEntry;
import com.lebhas.creativesaas.sharing.domain.ShareLink;
import org.springframework.stereotype.Component;

@Component
public class ShareLinkMapper {

    public RevisedShareLinkView toView(ShareLink entity) {
        return new RevisedShareLinkView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getGeneratedVersionId(),
                null,
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getAccessCount(),
                entity.isRevoked(),
                entity.getRevokedAt(),
                entity.getCreatedBy(),
                entity.getCreatedAt());
    }

    public RevisedShareLinkView toView(ShareLinkCacheEntry entry) {
        return new RevisedShareLinkView(
                entry.shareLinkId(),
                entry.workspaceId(),
                entry.generatedVersionId(),
                null,
                entry.tokenHash(),
                entry.expiresAt(),
                entry.accessCount(),
                entry.revoked(),
                entry.revokedAt(),
                entry.createdBy(),
                entry.createdAt());
    }

    public RevisedShareLinkView toCreationView(ShareLink entity, String rawToken) {
        return new RevisedShareLinkView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getGeneratedVersionId(),
                rawToken,
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getAccessCount(),
                entity.isRevoked(),
                entity.getRevokedAt(),
                entity.getCreatedBy(),
                entity.getCreatedAt());
    }
}
