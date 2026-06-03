package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.cache.AssetMetadataRedisCacheService;
import com.lebhas.creativesaas.asset.cache.dto.AssetMetadataCacheEntry;
import com.lebhas.creativesaas.workspace.application.WorkspaceActivityLogger;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssetCacheServiceTest {

    private final AssetMetadataRedisCacheService assetMetadataRedisCacheService = mock(AssetMetadataRedisCacheService.class);
    private final WorkspaceActivityLogger workspaceActivityLogger = mock(WorkspaceActivityLogger.class);
    private final AssetCacheService assetCacheService = new AssetCacheService(
            assetMetadataRedisCacheService,
            workspaceActivityLogger);

    @Test
    void shouldReturnCachedAssetWhenWorkspaceMatches() {
        UUID workspaceId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        AssetView cachedView = assetView(assetId, workspaceId);
        when(assetMetadataRedisCacheService.getAsset(workspaceId, assetId))
                .thenReturn(Optional.of(new AssetMetadataCacheEntry(
                        assetId,
                        workspaceId,
                        UUID.randomUUID(),
                        cachedView,
                        Instant.now())));
        AtomicBoolean loaderCalled = new AtomicBoolean(false);

        AssetView result = assetCacheService.getOrLoadAsset(workspaceId, assetId, () -> {
            loaderCalled.set(true);
            return assetView(assetId, workspaceId);
        });

        assertThat(result).isEqualTo(cachedView);
        assertThat(loaderCalled).isFalse();
    }

    @Test
    void shouldIgnoreCachedAssetWhenWorkspaceDoesNotMatch() {
        UUID requestedWorkspaceId = UUID.randomUUID();
        UUID cachedWorkspaceId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        AssetView loadedView = assetView(assetId, requestedWorkspaceId);
        when(assetMetadataRedisCacheService.getAsset(requestedWorkspaceId, assetId))
                .thenReturn(Optional.of(new AssetMetadataCacheEntry(
                        assetId,
                        cachedWorkspaceId,
                        UUID.randomUUID(),
                        assetView(assetId, cachedWorkspaceId),
                        Instant.now())));
        AtomicBoolean loaderCalled = new AtomicBoolean(false);

        AssetView result = assetCacheService.getOrLoadAsset(requestedWorkspaceId, assetId, () -> {
            loaderCalled.set(true);
            return loadedView;
        });

        assertThat(result).isEqualTo(loadedView);
        assertThat(loaderCalled).isTrue();
    }

    private AssetView assetView(UUID assetId, UUID workspaceId) {
        return new AssetView(
                assetId,
                workspaceId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                com.lebhas.creativesaas.asset.domain.AssetType.RAW_IMAGE,
                com.lebhas.creativesaas.asset.domain.AssetCategory.REFERENCE_IMAGE,
                "asset.png",
                "asset.png",
                com.lebhas.creativesaas.asset.domain.AssetFileType.IMAGE,
                "image/png",
                "png",
                1024L,
                com.lebhas.creativesaas.asset.domain.StorageProvider.R2,
                "assets",
                "workspaces/test/asset.png",
                null,
                null,
                null,
                "Asset",
                "Cached asset",
                UUID.randomUUID(),
                com.lebhas.creativesaas.asset.domain.PreviewStatus.READY,
                com.lebhas.creativesaas.asset.domain.ProcessingStatus.READY,
                com.lebhas.creativesaas.asset.domain.AssetStatus.READY,
                null,
                null,
                null,
                Set.of("catalog"),
                Map.of("source", "test"),
                Instant.now(),
                Instant.now());
    }
}
