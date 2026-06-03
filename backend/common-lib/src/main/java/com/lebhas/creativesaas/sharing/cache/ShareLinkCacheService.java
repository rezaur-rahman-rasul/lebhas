package com.lebhas.creativesaas.sharing.cache;

import com.lebhas.creativesaas.approval.cache.ApprovalRedisAccessSupport;
import com.lebhas.creativesaas.approval.cache.ApprovalRedisKeys;
import com.lebhas.creativesaas.approval.cache.ApprovalRedisOperationContext;
import com.lebhas.creativesaas.approval.cache.ApprovalRedisTtlStrategy;
import com.lebhas.creativesaas.sharing.domain.ShareLink;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ShareLinkCacheService {

    private final ApprovalRedisKeys approvalRedisKeys;
    private final ApprovalRedisAccessSupport approvalRedisAccessSupport;
    private final ApprovalRedisTtlStrategy approvalRedisTtlStrategy;

    public ShareLinkCacheService(
            ApprovalRedisKeys approvalRedisKeys,
            ApprovalRedisAccessSupport approvalRedisAccessSupport,
            ApprovalRedisTtlStrategy approvalRedisTtlStrategy
    ) {
        this.approvalRedisKeys = approvalRedisKeys;
        this.approvalRedisAccessSupport = approvalRedisAccessSupport;
        this.approvalRedisTtlStrategy = approvalRedisTtlStrategy;
    }

    public Optional<ShareLinkCacheEntry> getShareLink(String tokenHash) {
        String normalizedToken = normalizeToken(tokenHash);
        return approvalRedisAccessSupport.read(
                approvalRedisKeys.shareLink(normalizedToken),
                ShareLinkCacheEntry.class,
                "share_link_get",
                ApprovalRedisOperationContext.shareLink(null, null, normalizedToken));
    }

    public boolean cacheShareLink(ShareLink shareLink) {
        if (shareLink == null || shareLink.getTokenHash() == null) {
            return false;
        }
        return cacheShareLink(ShareLinkCacheEntry.from(shareLink));
    }

    public boolean cacheShareLink(ShareLinkCacheEntry entry) {
        if (entry == null || entry.tokenHash() == null) {
            return false;
        }
        String normalizedToken = normalizeToken(entry.tokenHash());
        return approvalRedisAccessSupport.write(
                approvalRedisKeys.shareLink(normalizedToken),
                entry,
                approvalRedisTtlStrategy.shareLinkTtl(entry.expiresAt()),
                "share_link_put",
                ApprovalRedisOperationContext.shareLink(entry.workspaceId(), entry.generatedVersionId(), normalizedToken));
    }

    public boolean invalidateShareLink(ShareLinkCacheEntry entry) {
        if (entry == null || entry.tokenHash() == null) {
            return false;
        }
        return invalidateShareLink(entry.workspaceId(), entry.generatedVersionId(), entry.tokenHash());
    }

    public boolean invalidateShareLink(ShareLink shareLink) {
        if (shareLink == null || shareLink.getTokenHash() == null) {
            return false;
        }
        return invalidateShareLink(shareLink.getWorkspaceId(), shareLink.getGeneratedVersionId(), shareLink.getTokenHash());
    }

    public boolean invalidateShareLink(java.util.UUID workspaceId, java.util.UUID generatedVersionId, String token) {
        String normalizedToken = normalizeToken(token);
        return approvalRedisAccessSupport.delete(
                approvalRedisKeys.shareLink(normalizedToken),
                "share_link_delete",
                ApprovalRedisOperationContext.shareLink(workspaceId, generatedVersionId, normalizedToken));
    }

    private static String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        return token.trim();
    }
}
