package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.asset.application.dto.AssetUrlView;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.download.application.DownloadService;
import com.lebhas.creativesaas.sharing.application.SecureTokenService;
import com.lebhas.creativesaas.sharing.application.ShareLinkService;
import com.lebhas.creativesaas.usage.application.ShareUsageAccessService;
import com.lebhas.creativesaas.usage.application.dto.ShareUsageTrackingCommand;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/share")
public class PublicShareController {

    private final ShareLinkService shareLinkService;
    private final ShareUsageAccessService shareUsageAccessService;
    private final SecureTokenService secureTokenService;
    private final DownloadService downloadService;
    private final DownloadRequestContextFactory downloadRequestContextFactory;

    public PublicShareController(
            ShareLinkService shareLinkService,
            ShareUsageAccessService shareUsageAccessService,
            SecureTokenService secureTokenService,
            DownloadService downloadService,
            DownloadRequestContextFactory downloadRequestContextFactory
    ) {
        this.shareLinkService = shareLinkService;
        this.shareUsageAccessService = shareUsageAccessService;
        this.secureTokenService = secureTokenService;
        this.downloadService = downloadService;
        this.downloadRequestContextFactory = downloadRequestContextFactory;
    }

    @GetMapping("/{token}")
    public ApiResponse<PublicShareMetadataResponse> validate(@PathVariable String token, HttpServletRequest request) {
        ShareLinkService.ResolvedShareLink shareLink = shareLinkService.resolvePublicShareLink(token, null);
        shareUsageAccessService.recordPublicShareAccess(new ShareUsageTrackingCommand(
                secureTokenService.hashToken(token),
                null,
                request == null ? null : request.getRemoteAddr(),
                request == null ? null : request.getHeader("User-Agent"),
                null,
                null));
        return ApiResponse.success(new PublicShareMetadataResponse(
                shareLink.generatedVersionId(),
                shareLink.workspaceId(),
                shareLink.generatedVersion().getVersionName(),
                shareLink.generatedVersion().getVersionNumber(),
                shareLink.expiresAt(),
                shareLink.passwordProtected()));
    }

    @PostMapping("/{token}/download-url")
    public ApiResponse<GeneratedVersionPreviewUrlResponse> downloadUrl(
            @PathVariable String token,
            @RequestBody(required = false) PublicShareDownloadRequest body,
            HttpServletRequest request
    ) {
        AssetUrlView url = downloadService.requestPublicShareDownload(
                token,
                body == null ? null : body.password(),
                downloadRequestContextFactory.create(request, "public-share-download"));
        return ApiResponse.success(new GeneratedVersionPreviewUrlResponse(
                null,
                url.url(),
                url.type(),
                url.cdnUrl(),
                url.cached(),
                url.generatedAt(),
                url.expiresAt()));
    }
}
