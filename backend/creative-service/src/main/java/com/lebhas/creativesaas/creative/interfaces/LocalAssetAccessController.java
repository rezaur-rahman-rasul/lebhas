package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.asset.application.AssetManagementService;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.storage.LocalAssetAccessMode;
import com.lebhas.creativesaas.asset.storage.LocalAssetContentAccessor;
import com.lebhas.creativesaas.asset.storage.LocalSignedAssetUrlService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.download.application.AssetDownloadTrackingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/internal/storage/local/assets")
@ConditionalOnProperty(prefix = "platform.storage", name = "provider", havingValue = "LOCAL")
public class LocalAssetAccessController {

    private final AssetManagementService assetManagementService;
    private final LocalSignedAssetUrlService localSignedAssetUrlService;
    private final LocalAssetContentAccessor localAssetContentAccessor;
    private final AssetDownloadTrackingService assetDownloadTrackingService;

    public LocalAssetAccessController(
            AssetManagementService assetManagementService,
            LocalSignedAssetUrlService localSignedAssetUrlService,
            LocalAssetContentAccessor localAssetContentAccessor,
            AssetDownloadTrackingService assetDownloadTrackingService
    ) {
        this.assetManagementService = assetManagementService;
        this.localSignedAssetUrlService = localSignedAssetUrlService;
        this.localAssetContentAccessor = localAssetContentAccessor;
        this.assetDownloadTrackingService = assetDownloadTrackingService;
    }

    @GetMapping("/{assetId}/preview")
    public ResponseEntity<Resource> preview(
            @PathVariable UUID assetId,
            @RequestParam long expiresAt,
            @RequestParam String signature,
            HttpServletRequest request
    ) {
        return serve(assetId, expiresAt, signature, LocalAssetAccessMode.PREVIEW, request);
    }

    @GetMapping("/{assetId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable UUID assetId,
            @RequestParam long expiresAt,
            @RequestParam String signature,
            HttpServletRequest request
    ) {
        return serve(assetId, expiresAt, signature, LocalAssetAccessMode.DOWNLOAD, request);
    }

    private ResponseEntity<Resource> serve(
            UUID assetId,
            long expiresAt,
            String signature,
            LocalAssetAccessMode mode,
            HttpServletRequest request
    ) {
        localSignedAssetUrlService.verify(assetId, mode, expiresAt, signature);
        AssetEntity asset = assetManagementService.requireAssetForSignedAccess(assetId);
        Resource resource = localAssetContentAccessor.open(asset);
        if (!resource.exists()) {
            throw new BusinessException(ErrorCode.ASSET_NOT_FOUND);
        }
        if (mode == LocalAssetAccessMode.DOWNLOAD) {
            assetDownloadTrackingService.recordDownloadCompleted(
                    asset,
                    null,
                    mode.name().toLowerCase(),
                    request.getRemoteAddr(),
                    request.getHeader(HttpHeaders.USER_AGENT));
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(asset.getOriginalFileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        ContentDisposition disposition = mode == LocalAssetAccessMode.DOWNLOAD
                ? ContentDisposition.attachment()
                .filename(asset.getOriginalFileName(), StandardCharsets.UTF_8)
                .build()
                : ContentDisposition.inline()
                .filename(asset.getOriginalFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
