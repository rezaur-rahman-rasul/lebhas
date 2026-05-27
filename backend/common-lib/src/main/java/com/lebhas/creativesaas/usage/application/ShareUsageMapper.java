package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.usage.application.dto.ShareUsageView;
import com.lebhas.creativesaas.usage.domain.ShareUsageLog;
import com.lebhas.creativesaas.profile.application.SafeProfileDisplayService;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ShareUsageMapper {

    private SafeProfileDisplayService safeProfileDisplayService;

    @Autowired(required = false)
    public void setSafeProfileDisplayService(SafeProfileDisplayService safeProfileDisplayService) {
        this.safeProfileDisplayService = safeProfileDisplayService;
    }

    public ShareUsageView toView(ShareUsageLog log, long accessCount) {
        return new ShareUsageView(
                log.getId(),
                log.getWorkspaceId(),
                log.getShareLinkId(),
                log.getGeneratedVersionId(),
                log.getAccessedByUserId(),
                safeDisplay(log.getWorkspaceId(), log.getAccessedByUserId()),
                log.getAccessIp(),
                log.getUserAgent(),
                log.getReferrer(),
                accessCount,
                log.getCreatedAt());
    }

    private SafeProfileDisplayView safeDisplay(UUID workspaceId, UUID userId) {
        return safeProfileDisplayService == null ? null : safeProfileDisplayService.forUserInWorkspace(workspaceId, userId);
    }
}
