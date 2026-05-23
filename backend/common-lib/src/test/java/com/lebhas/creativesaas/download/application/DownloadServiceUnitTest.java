package com.lebhas.creativesaas.download.application;

import com.lebhas.creativesaas.asset.application.AssetService;
import com.lebhas.creativesaas.asset.application.dto.AssetUrlView;
import com.lebhas.creativesaas.asset.cache.AssetCacheTtlStrategy;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.asset.storage.StorageProperties;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.download.application.dto.DownloadRequestContext;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.redis.RedisSignedUrlCache;
import com.lebhas.creativesaas.sharing.application.ShareLinkService;
import com.lebhas.creativesaas.storage.application.StorageFileService;
import com.lebhas.creativesaas.usage.application.ShareUsageAccessService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DownloadServiceUnitTest {

    private final WorkspaceAuthorizationService workspaceAuthorizationService = mock(WorkspaceAuthorizationService.class);
    private final AssetService assetService = mock(AssetService.class);
    private final DownloadHistoryService downloadHistoryService = mock(DownloadHistoryService.class);
    private final ShareLinkService shareLinkService = mock(ShareLinkService.class);
    private final StorageFileService storageFileService = mock(StorageFileService.class);
    private final StorageService storageService = mock(StorageService.class);
    private final RedisSignedUrlCache redisSignedUrlCache = mock(RedisSignedUrlCache.class);
    private final AssetCacheTtlStrategy assetCacheTtlStrategy = mock(AssetCacheTtlStrategy.class);
    private final StorageProperties storageProperties = new StorageProperties();
    private final ShareUsageAccessService shareUsageAccessService = mock(ShareUsageAccessService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-15T10:00:00Z"), ZoneOffset.UTC);

    private final DownloadService downloadService = new DownloadService(
            workspaceAuthorizationService,
            assetService,
            downloadHistoryService,
            shareLinkService,
            storageFileService,
            storageService,
            redisSignedUrlCache,
            assetCacheTtlStrategy,
            storageProperties,
            shareUsageAccessService,
            clock);

    @Test
    void shouldRecordDownloadImmediatelyForNonLocalStorage() {
        UUID workspaceId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        AssetEntity asset = mock(AssetEntity.class);
        AssetUrlView urlView = new AssetUrlView(
                "https://cdn.example.com/download",
                "download",
                null,
                false,
                Instant.now(clock),
                Instant.now(clock).plusSeconds(60));
        WorkspaceAuthorizationService.WorkspaceAccess access = mock(WorkspaceAuthorizationService.WorkspaceAccess.class);
        when(asset.getStorageProvider()).thenReturn(StorageProvider.R2);
        when(assetService.requireAsset(workspaceId, assetId)).thenReturn(asset);
        when(assetService.generateDownloadUrl(workspaceId, assetId)).thenReturn(urlView);
        when(workspaceAuthorizationService.requirePermission(
                workspaceId,
                com.lebhas.creativesaas.common.security.Permission.CREATIVE_DOWNLOAD)).thenReturn(access);
        when(access.currentUser()).thenReturn(new com.lebhas.creativesaas.common.security.context.CurrentUser(
                UUID.randomUUID(),
                workspaceId,
                null,
                "user@example.com",
                Set.of(com.lebhas.creativesaas.common.security.Role.ADMIN),
                Set.of(com.lebhas.creativesaas.common.security.Permission.CREATIVE_DOWNLOAD),
                UUID.randomUUID().toString(),
                Instant.now(clock).plusSeconds(300)));

        downloadService.requestAssetDownload(workspaceId, assetId, new DownloadRequestContext("download", "127.0.0.1", "JUnit"));

        verify(downloadHistoryService).recordAssetDownload(asset, access.currentUser().userId(), new DownloadRequestContext("download", "127.0.0.1", "JUnit"));
    }

    @Test
    void shouldNotRecordDownloadUntilDeliveryForLocalStorage() {
        UUID workspaceId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        AssetEntity asset = mock(AssetEntity.class);
        AssetUrlView urlView = new AssetUrlView(
                "http://localhost/internal/storage/local/assets/" + assetId + "/download",
                "download",
                null,
                false,
                Instant.now(clock),
                Instant.now(clock).plusSeconds(60));
        WorkspaceAuthorizationService.WorkspaceAccess access = mock(WorkspaceAuthorizationService.WorkspaceAccess.class);
        when(asset.getStorageProvider()).thenReturn(StorageProvider.LOCAL);
        when(assetService.requireAsset(workspaceId, assetId)).thenReturn(asset);
        when(assetService.generateDownloadUrl(workspaceId, assetId)).thenReturn(urlView);
        when(workspaceAuthorizationService.requirePermission(
                workspaceId,
                com.lebhas.creativesaas.common.security.Permission.CREATIVE_DOWNLOAD)).thenReturn(access);
        when(access.currentUser()).thenReturn(new com.lebhas.creativesaas.common.security.context.CurrentUser(
                UUID.randomUUID(),
                workspaceId,
                null,
                "user@example.com",
                Set.of(com.lebhas.creativesaas.common.security.Role.ADMIN),
                Set.of(com.lebhas.creativesaas.common.security.Permission.CREATIVE_DOWNLOAD),
                UUID.randomUUID().toString(),
                Instant.now(clock).plusSeconds(300)));

        downloadService.requestAssetDownload(workspaceId, assetId, new DownloadRequestContext("download", "127.0.0.1", "JUnit"));

        verify(downloadHistoryService, never()).recordAssetDownload(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
