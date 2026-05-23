package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.sharing.domain.ShareLink;
import com.lebhas.creativesaas.usage.application.dto.ShareUsageTrackingCommand;
import com.lebhas.creativesaas.usage.application.dto.ShareUsageView;
import com.lebhas.creativesaas.usage.cache.ShareCounterCacheService;
import com.lebhas.creativesaas.usage.domain.ShareUsageLog;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import com.lebhas.creativesaas.usage.event.ShareAccessedEventDto;
import com.lebhas.creativesaas.usage.event.UsageBillingEventProducer;
import com.lebhas.creativesaas.usage.infrastructure.persistence.ShareUsageLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class ShareUsageTrackingService {

    private final ShareUsageLogRepository shareUsageLogRepository;
    private final WorkspaceUsageSummaryService workspaceUsageSummaryService;
    private final ShareCounterCacheService shareCounterCacheService;
    private final UsageBillingEventProducer usageBillingEventProducer;
    private final ShareUsageMapper shareUsageMapper;

    public ShareUsageTrackingService(
            ShareUsageLogRepository shareUsageLogRepository,
            WorkspaceUsageSummaryService workspaceUsageSummaryService,
            ShareCounterCacheService shareCounterCacheService,
            UsageBillingEventProducer usageBillingEventProducer,
            ShareUsageMapper shareUsageMapper
    ) {
        this.shareUsageLogRepository = shareUsageLogRepository;
        this.workspaceUsageSummaryService = workspaceUsageSummaryService;
        this.shareCounterCacheService = shareCounterCacheService;
        this.usageBillingEventProducer = usageBillingEventProducer;
        this.shareUsageMapper = shareUsageMapper;
    }

    @Transactional
    public ShareUsageView trackShareAccess(ShareLink shareLink, ShareUsageTrackingCommand command) {
        ShareUsageLog usageLog = shareUsageLogRepository.save(ShareUsageLog.create(
                shareLink.getWorkspaceId(),
                shareLink.getId(),
                shareLink.getGeneratedVersionId(),
                command.accessedByUserId(),
                normalize(command.accessIp(), 80),
                normalize(command.userAgent(), 500),
                normalize(command.referrer(), 1000)));
        WorkspaceUsageSummary summary = workspaceUsageSummaryService.getOrCreateSummary(
                shareLink.getWorkspaceId(),
                normalizeMonth(command.usageMonth()));
        summary.recordPublicShareAccess();
        workspaceUsageSummaryService.recordSummaryMutation(summary, "SHARE_USAGE_LOG", usageLog.getId(), "SHARE_ACCESSED");
        shareCounterCacheService.put(summary.getWorkspaceId(), summary.getUsageMonth(), summary.getTotalPublicShares());
        publishShareAccessTracked(usageLog, shareLink.getAccessCount(), summary.getUsageMonth());
        return shareUsageMapper.toView(usageLog, shareLink.getAccessCount());
    }

    private void publishShareAccessTracked(ShareUsageLog usageLog, long accessCount, LocalDate usageMonth) {
        usageBillingEventProducer.publishShareAccessed(new ShareAccessedEventDto(
                usageLog.getWorkspaceId(),
                usageLog.getId(),
                usageLog.getShareLinkId(),
                usageLog.getGeneratedVersionId(),
                usageLog.getAccessedByUserId(),
                accessCount,
                usageMonth,
                true,
                Instant.now()));
    }

    private LocalDate normalizeMonth(LocalDate usageMonth) {
        return (usageMonth == null ? LocalDate.now(ZoneOffset.UTC) : usageMonth).withDayOfMonth(1);
    }

    private String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
