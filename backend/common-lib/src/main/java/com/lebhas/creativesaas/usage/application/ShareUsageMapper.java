package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.usage.application.dto.ShareUsageView;
import com.lebhas.creativesaas.usage.domain.ShareUsageLog;
import org.springframework.stereotype.Component;

@Component
public class ShareUsageMapper {

    public ShareUsageView toView(ShareUsageLog log, long accessCount) {
        return new ShareUsageView(
                log.getId(),
                log.getWorkspaceId(),
                log.getShareLinkId(),
                log.getGeneratedVersionId(),
                log.getAccessedByUserId(),
                log.getAccessIp(),
                log.getUserAgent(),
                log.getReferrer(),
                accessCount,
                log.getCreatedAt());
    }
}
