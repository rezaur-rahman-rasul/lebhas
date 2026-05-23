package com.lebhas.creativesaas.creative;

import com.lebhas.creativesaas.asset.application.AssetManagementService;
import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.storage.LocalAssetContentAccessor;
import com.lebhas.creativesaas.asset.storage.LocalSignedAssetUrlService;
import com.lebhas.creativesaas.creative.interfaces.LocalAssetAccessController;
import com.lebhas.creativesaas.download.application.AssetDownloadTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalAssetAccessControllerTest {

    @Test
    void shouldTrackAnonymousDownloaderForSignedLocalDownloads() {
        AssetManagementService assetManagementService = mock(AssetManagementService.class);
        LocalSignedAssetUrlService localSignedAssetUrlService = mock(LocalSignedAssetUrlService.class);
        LocalAssetContentAccessor localAssetContentAccessor = mock(LocalAssetContentAccessor.class);
        AssetDownloadTrackingService assetDownloadTrackingService = mock(AssetDownloadTrackingService.class);
        LocalAssetAccessController controller = new LocalAssetAccessController(
                assetManagementService,
                localSignedAssetUrlService,
                localAssetContentAccessor,
                assetDownloadTrackingService);

        UUID assetId = UUID.randomUUID();
        AssetEntity asset = AssetEntity.createUploading(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                AssetType.RAW_IMAGE,
                AssetCategory.REFERENCE_IMAGE,
                "asset.png",
                "Asset",
                null,
                Set.of(),
                UUID.randomUUID(),
                null,
                com.lebhas.creativesaas.asset.domain.StorageProvider.LOCAL);
        asset.completeUpload(
                "asset.png",
                com.lebhas.creativesaas.asset.domain.AssetFileType.IMAGE,
                "image/png",
                "png",
                128L,
                com.lebhas.creativesaas.asset.domain.StorageProvider.LOCAL,
                "creative-saas-assets",
                "assets/workspaces/test.png",
                "checksum",
                null,
                null,
                null,
                1,
                1,
                null);
        asset.markPreviewReady();
        org.springframework.test.util.ReflectionTestUtils.setField(asset, "id", assetId);
        when(assetManagementService.requireAssetForSignedAccess(assetId)).thenReturn(asset);
        when(localAssetContentAccessor.open(asset)).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");

        controller.download(assetId, 1L, "signature", request);

        verify(localSignedAssetUrlService).verify(assetId, com.lebhas.creativesaas.asset.storage.LocalAssetAccessMode.DOWNLOAD, 1L, "signature");
        verify(assetDownloadTrackingService).recordDownloadCompleted(asset, null, "download", "127.0.0.1", "JUnit");
    }
}
