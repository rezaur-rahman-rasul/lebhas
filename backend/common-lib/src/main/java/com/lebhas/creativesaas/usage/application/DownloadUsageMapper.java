package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.usage.application.dto.DownloadUsageView;
import com.lebhas.creativesaas.usage.domain.DownloadUsageLog;
import org.springframework.stereotype.Component;

@Component
public class DownloadUsageMapper {

    public DownloadUsageView toView(DownloadUsageLog log) {
        return new DownloadUsageView(
                log.getId(),
                log.getWorkspaceId(),
                log.getGeneratedVersionId(),
                log.getAssetId(),
                log.getDownloadedBy(),
                log.getDownloadType(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt());
    }
}
