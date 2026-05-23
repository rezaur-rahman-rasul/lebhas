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
                entity.getToken(),
                entity.getExpiresAt(),
                entity.getAccessCount(),
                entity.getCreatedBy(),
                entity.getCreatedAt());
    }

    public RevisedShareLinkView toView(ShareLinkCacheEntry entry) {
        return new RevisedShareLinkView(
                entry.shareLinkId(),
                entry.workspaceId(),
                entry.generatedVersionId(),
                entry.token(),
                entry.expiresAt(),
                entry.accessCount(),
                entry.createdBy(),
                entry.createdAt());
    }
}
