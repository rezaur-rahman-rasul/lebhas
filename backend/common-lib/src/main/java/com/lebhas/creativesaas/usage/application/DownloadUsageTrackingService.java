package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.usage.application.dto.DownloadUsageTrackingCommand;
import com.lebhas.creativesaas.usage.application.dto.DownloadUsageView;
import com.lebhas.creativesaas.usage.cache.DownloadCounterCacheService;
import com.lebhas.creativesaas.usage.domain.DownloadUsageLog;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import com.lebhas.creativesaas.usage.event.DownloadTrackedEventDto;
import com.lebhas.creativesaas.usage.event.UsageBillingEventProducer;
import com.lebhas.creativesaas.usage.infrastructure.persistence.DownloadUsageLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class DownloadUsageTrackingService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final GeneratedVersionRepository generatedVersionRepository;
    private final DownloadUsageLogRepository downloadUsageLogRepository;
    private final WorkspaceUsageSummaryService workspaceUsageSummaryService;
    private final DownloadCounterCacheService downloadCounterCacheService;
    private final UsageBillingEventProducer usageBillingEventProducer;
    private final DownloadUsageMapper downloadUsageMapper;

    public DownloadUsageTrackingService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            GeneratedVersionRepository generatedVersionRepository,
            DownloadUsageLogRepository downloadUsageLogRepository,
            WorkspaceUsageSummaryService workspaceUsageSummaryService,
            DownloadCounterCacheService downloadCounterCacheService,
            UsageBillingEventProducer usageBillingEventProducer,
            DownloadUsageMapper downloadUsageMapper
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.generatedVersionRepository = generatedVersionRepository;
        this.downloadUsageLogRepository = downloadUsageLogRepository;
        this.workspaceUsageSummaryService = workspaceUsageSummaryService;
        this.downloadCounterCacheService = downloadCounterCacheService;
        this.usageBillingEventProducer = usageBillingEventProducer;
        this.downloadUsageMapper = downloadUsageMapper;
    }

    @Transactional
    public DownloadUsageView trackGeneratedVersionDownload(DownloadUsageTrackingCommand command) {
        UUID workspaceId = require(command.workspaceId(), "workspaceId");
        UUID generatedVersionId = require(command.generatedVersionId(), "generatedVersionId");
        if (command.downloadedBy() != null) {
            workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREATIVE_DOWNLOAD);
        }
        GeneratedVersionEntity generatedVersion = generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATED_VERSION_NOT_FOUND));
        UUID assetId = command.assetId() == null ? generatedVersion.getAssetId() : command.assetId();

        DownloadUsageLog usageLog = downloadUsageLogRepository.save(DownloadUsageLog.create(
                workspaceId,
                generatedVersionId,
                assetId,
                command.downloadedBy(),
                normalize(command.downloadType(), "download"),
                normalize(command.ipAddress(), null),
                normalize(command.userAgent(), null)));
        WorkspaceUsageSummary summary = workspaceUsageSummaryService.getOrCreateSummary(workspaceId, normalizeMonth(command.usageMonth()));
        summary.recordDownload();
        workspaceUsageSummaryService.recordSummaryMutation(summary, "DOWNLOAD_USAGE_LOG", usageLog.getId(), "DOWNLOAD_TRACKED");
        downloadCounterCacheService.put(summary.getWorkspaceId(), summary.getUsageMonth(), summary.getTotalDownloads());
        publishDownloadTracked(usageLog, summary.getUsageMonth());
        return downloadUsageMapper.toView(usageLog);
    }

    private void publishDownloadTracked(DownloadUsageLog usageLog, LocalDate usageMonth) {
        usageBillingEventProducer.publishDownloadTracked(new DownloadTrackedEventDto(
                usageLog.getWorkspaceId(),
                usageLog.getId(),
                usageLog.getGeneratedVersionId(),
                usageLog.getAssetId(),
                usageLog.getDownloadedBy(),
                usageLog.getDownloadType(),
                usageMonth,
                true,
                Instant.now()));
    }

    private LocalDate normalizeMonth(LocalDate usageMonth) {
        return (usageMonth == null ? LocalDate.now(ZoneOffset.UTC) : usageMonth).withDayOfMonth(1);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }

    private <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
