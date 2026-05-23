package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.constants.CommonHeaders;
import com.lebhas.creativesaas.download.application.DownloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/downloads")
@Tag(name = "Downloads")
@SecurityRequirement(name = "bearerAuth")
public class Day4DownloadController {

    private final DownloadService downloadService;
    private final WorkspaceRequestResolver workspaceRequestResolver;
    private final DownloadRequestContextFactory downloadRequestContextFactory;
    private final Day4ApiMapper day4ApiMapper;

    public Day4DownloadController(
            DownloadService downloadService,
            WorkspaceRequestResolver workspaceRequestResolver,
            DownloadRequestContextFactory downloadRequestContextFactory,
            Day4ApiMapper day4ApiMapper
    ) {
        this.downloadService = downloadService;
        this.workspaceRequestResolver = workspaceRequestResolver;
        this.downloadRequestContextFactory = downloadRequestContextFactory;
        this.day4ApiMapper = day4ApiMapper;
    }

    @GetMapping("/{assetId}")
    @PreAuthorize("hasAuthority('CREATIVE_DOWNLOAD')")
    @Operation(summary = "Generate a tracked download URL for an asset")
    @Parameter(
            name = CommonHeaders.WORKSPACE_ID,
            in = ParameterIn.HEADER,
            required = false,
            description = "Workspace context. Optional when the access token is already bound to a workspace; required for master or cross-workspace access.",
            schema = @Schema(type = "string", format = "uuid"))
    public ApiResponse<AssetDownloadResponse> requestDownload(
            @PathVariable UUID assetId,
            HttpServletRequest httpServletRequest
    ) {
        UUID workspaceId = workspaceRequestResolver.requireWorkspaceId();
        return ApiResponse.success(
                "Download URL generated",
                day4ApiMapper.toAssetDownloadResponse(
                        assetId,
                        downloadService.requestAssetDownload(
                                workspaceId,
                                assetId,
                                downloadRequestContextFactory.create(httpServletRequest, "download"))));
    }
}
