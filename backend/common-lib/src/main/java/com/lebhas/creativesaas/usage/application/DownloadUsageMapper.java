package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.usage.application.dto.DownloadUsageView;
import com.lebhas.creativesaas.usage.domain.DownloadUsageLog;
import com.lebhas.creativesaas.profile.application.SafeProfileDisplayService;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DownloadUsageMapper {

    private SafeProfileDisplayService safeProfileDisplayService;

    @Autowired(required = false)
    public void setSafeProfileDisplayService(SafeProfileDisplayService safeProfileDisplayService) {
        this.safeProfileDisplayService = safeProfileDisplayService;
    }

    public DownloadUsageView toView(DownloadUsageLog log) {
        return new DownloadUsageView(
                log.getId(),
                log.getWorkspaceId(),
                log.getGeneratedVersionId(),
                log.getAssetId(),
                log.getDownloadedBy(),
                safeDisplay(log.getWorkspaceId(), log.getDownloadedBy()),
                log.getDownloadType(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt());
    }

    private SafeProfileDisplayView safeDisplay(UUID workspaceId, UUID userId) {
        return safeProfileDisplayService == null ? null : safeProfileDisplayService.forUserInWorkspace(workspaceId, userId);
    }
}
