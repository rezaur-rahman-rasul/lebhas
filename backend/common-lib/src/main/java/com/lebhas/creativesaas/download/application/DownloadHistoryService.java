package com.lebhas.creativesaas.download.application;

import com.lebhas.creativesaas.asset.application.AssetService;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.download.application.dto.DownloadHistoryView;
import com.lebhas.creativesaas.download.application.dto.DownloadRequestContext;
import com.lebhas.creativesaas.download.domain.DownloadLogEntity;
import com.lebhas.creativesaas.download.infrastructure.persistence.DownloadLogRepository;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.usage.application.DownloadUsageTrackingService;
import com.lebhas.creativesaas.usage.application.dto.DownloadUsageTrackingCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class DownloadHistoryService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final DownloadLogRepository downloadLogRepository;
    private final AssetDownloadTrackingService assetDownloadTrackingService;
    private final AssetService assetService;
    private final GeneratedVersionRepository generatedVersionRepository;
    private final DownloadUsageTrackingService downloadUsageTrackingService;

    public DownloadHistoryService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            DownloadLogRepository downloadLogRepository,
            AssetDownloadTrackingService assetDownloadTrackingService,
            AssetService assetService,
            GeneratedVersionRepository generatedVersionRepository,
            DownloadUsageTrackingService downloadUsageTrackingService
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.downloadLogRepository = downloadLogRepository;
        this.assetDownloadTrackingService = assetDownloadTrackingService;
        this.assetService = assetService;
        this.generatedVersionRepository = generatedVersionRepository;
        this.downloadUsageTrackingService = downloadUsageTrackingService;
    }

    @Transactional
    public void recordAssetDownload(AssetEntity asset, UUID actorUserId, DownloadRequestContext requestContext) {
        assetDownloadTrackingService.recordDownloadCompleted(
                asset,
                actorUserId,
                normalizeSource(requestContext),
                normalizeIp(requestContext),
                normalizeUserAgent(requestContext));
    }

    @Transactional
    public void recordGeneratedVersionDownload(
            UUID workspaceId,
            UUID generatedVersionId,
            UUID actorUserId,
            DownloadRequestContext requestContext
    ) {
        GeneratedVersionEntity generatedVersion = requireGeneratedVersion(workspaceId, generatedVersionId);
        downloadLogRepository.save(DownloadLogEntity.create(
                workspaceId,
                generatedVersionId,
                actorUserId,
                normalizeSource(requestContext),
                normalizeIp(requestContext),
                normalizeUserAgent(requestContext)));
        downloadUsageTrackingService.trackGeneratedVersionDownload(new DownloadUsageTrackingCommand(
                workspaceId,
                generatedVersionId,
                generatedVersion.getAssetId(),
                actorUserId,
                normalizeSource(requestContext),
                normalizeIp(requestContext),
                normalizeUserAgent(requestContext),
                null));
    }

    @Transactional(readOnly = true)
    public List<DownloadHistoryView> listAssetDownloadHistory(UUID workspaceId, UUID assetId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREATIVE_DOWNLOAD);
        assetService.requireAsset(workspaceId, assetId);
        return downloadLogRepository.findAllByWorkspaceIdAndAssetIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId, assetId)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DownloadHistoryView> listGeneratedVersionDownloadHistory(UUID workspaceId, UUID generatedVersionId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREATIVE_DOWNLOAD);
        requireGeneratedVersion(workspaceId, generatedVersionId);
        return downloadLogRepository.findAllByWorkspaceIdAndGeneratedVersionIdAndDeletedFalseOrderByCreatedAtDesc(
                        workspaceId,
                        generatedVersionId)
                .stream()
                .map(this::toView)
                .toList();
    }

    private GeneratedVersionEntity requireGeneratedVersion(UUID workspaceId, UUID generatedVersionId) {
        return generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, workspaceId)
                .orElseThrow(() -> new com.lebhas.creativesaas.common.exception.BusinessException(
                        com.lebhas.creativesaas.common.exception.ErrorCode.GENERATED_VERSION_NOT_FOUND));
    }

    private DownloadHistoryView toView(DownloadLogEntity entity) {
        return new DownloadHistoryView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getGeneratedVersionId(),
                entity.getAssetId(),
                entity.getDownloadedBy(),
                entity.getDownloadSource(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getDownloadedAt());
    }

    private String normalizeSource(DownloadRequestContext requestContext) {
        if (requestContext == null || !StringUtils.hasText(requestContext.downloadSource())) {
            return "download";
        }
        return requestContext.downloadSource().trim();
    }

    private String normalizeIp(DownloadRequestContext requestContext) {
        if (requestContext == null || !StringUtils.hasText(requestContext.ipAddress())) {
            return null;
        }
        return requestContext.ipAddress().trim();
    }

    private String normalizeUserAgent(DownloadRequestContext requestContext) {
        if (requestContext == null || !StringUtils.hasText(requestContext.userAgent())) {
            return null;
        }
        String userAgent = requestContext.userAgent().trim();
        return userAgent.length() <= 500 ? userAgent : userAgent.substring(0, 500);
    }
}
