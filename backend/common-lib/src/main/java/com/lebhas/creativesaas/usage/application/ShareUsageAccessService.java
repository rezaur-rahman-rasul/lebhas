package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.sharing.cache.ShareLinkCacheService;
import com.lebhas.creativesaas.sharing.domain.ShareLink;
import com.lebhas.creativesaas.sharing.infrastructure.persistence.ShareLinkRepository;
import com.lebhas.creativesaas.usage.application.PlanUsagePolicyResolver.PlanUsagePolicy;
import com.lebhas.creativesaas.usage.application.dto.ShareUsageTrackingCommand;
import com.lebhas.creativesaas.usage.application.dto.ShareUsageView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;

@Service
public class ShareUsageAccessService {

    private final ShareLinkRepository shareLinkRepository;
    private final PlanUsagePolicyResolver planUsagePolicyResolver;
    private final ShareLinkCacheService shareLinkCacheService;
    private final ShareUsageTrackingService shareUsageTrackingService;
    private final Clock clock;

    public ShareUsageAccessService(
            ShareLinkRepository shareLinkRepository,
            PlanUsagePolicyResolver planUsagePolicyResolver,
            ShareLinkCacheService shareLinkCacheService,
            ShareUsageTrackingService shareUsageTrackingService,
            Clock clock
    ) {
        this.shareLinkRepository = shareLinkRepository;
        this.planUsagePolicyResolver = planUsagePolicyResolver;
        this.shareLinkCacheService = shareLinkCacheService;
        this.shareUsageTrackingService = shareUsageTrackingService;
        this.clock = clock;
    }

    @Transactional
    public ShareUsageView recordPublicShareAccess(ShareUsageTrackingCommand command) {
        String token = normalizeToken(command.token());
        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Share link not found"));
        validateNotExpired(shareLink);
        PlanUsagePolicy policy = planUsagePolicyResolver.resolve(shareLink.getWorkspaceId());
        if (!policy.featurePolicy().isAllowPublicShareLinks()) {
            throw new BusinessException(ErrorCode.PLAN_FEATURE_DISABLED, "Public share links are not enabled for the workspace plan");
        }
        shareLink.incrementAccessCount();
        ShareLink saved = shareLinkRepository.save(shareLink);
        shareLinkCacheService.cacheShareLink(saved);
        return shareUsageTrackingService.trackShareAccess(saved, command);
    }

    private void validateNotExpired(ShareLink shareLink) {
        if (shareLink.getExpiresAt() != null && shareLink.getExpiresAt().isBefore(clock.instant())) {
            shareLinkCacheService.invalidateShareLink(shareLink);
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Share link has expired");
        }
    }

    private String normalizeToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        return token.trim();
    }
}
