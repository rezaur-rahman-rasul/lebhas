package com.lebhas.creativesaas.download.application;

import com.lebhas.creativesaas.asset.application.AssetActivityLogger;
import com.lebhas.creativesaas.asset.application.AssetEventPublisher;
import com.lebhas.creativesaas.asset.cache.AssetHotRedisCacheService;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.download.domain.DownloadLogEntity;
import com.lebhas.creativesaas.download.infrastructure.persistence.DownloadLogRepository;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AssetDownloadTrackingService {

    private final DownloadLogRepository downloadLogRepository;
    private final AssetHotRedisCacheService assetHotRedisCacheService;
    private final AssetEventPublisher assetEventPublisher;
    private final AssetActivityLogger assetActivityLogger;

    public AssetDownloadTrackingService(
            DownloadLogRepository downloadLogRepository,
            AssetHotRedisCacheService assetHotRedisCacheService,
            AssetEventPublisher assetEventPublisher,
            AssetActivityLogger assetActivityLogger
    ) {
        this.downloadLogRepository = downloadLogRepository;
        this.assetHotRedisCacheService = assetHotRedisCacheService;
        this.assetEventPublisher = assetEventPublisher;
        this.assetActivityLogger = assetActivityLogger;
    }

    @Transactional
    public void recordDownloadCompleted(
            AssetEntity asset,
            UUID actorUserId,
            String downloadType,
            String ipAddress,
            String userAgent
    ) {
        downloadLogRepository.save(DownloadLogEntity.createForAsset(
                asset.getWorkspaceId(),
                asset.getId(),
                actorUserId,
                downloadType,
                ipAddress,
                userAgent));
        assetHotRedisCacheService.recordDownload(asset.getWorkspaceId(), asset.getId(), downloadType);
        assetActivityLogger.logDownloadCompleted(asset.getWorkspaceId(), asset.getId(), actorUserId, downloadType);
        assetEventPublisher.publish(
                KafkaTopicConstants.ASSET_DOWNLOAD_COMPLETED,
                asset.getWorkspaceId(),
                asset.getId(),
                Map.of(
                        "workspaceId", asset.getWorkspaceId().toString(),
                        "assetId", asset.getId().toString(),
                        "downloadType", downloadType,
                        "actorUserId", actorUserId == null ? "anonymous" : actorUserId.toString()));
    }
}
