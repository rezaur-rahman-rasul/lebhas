package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.download.application.DownloadHistoryService;
import com.lebhas.creativesaas.download.application.DownloadService;
import com.lebhas.creativesaas.download.application.dto.DownloadHistoryView;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionReviewService;
import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionApprovalActionCommand;
import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionApprovalHistoryView;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionQueryService;
import com.lebhas.creativesaas.sharing.application.ShareLinkService;
import com.lebhas.creativesaas.sharing.application.dto.CreateRevisedShareLinkCommand;
import com.lebhas.creativesaas.sharing.application.dto.RevisedShareLinkView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
@Tag(name = "Generated Versions")
@SecurityRequirement(name = "bearerAuth")
public class GeneratedVersionController {

    private final GeneratedVersionQueryService generatedVersionQueryService;
    private final GeneratedVersionReviewService generatedVersionReviewService;
    private final DownloadService downloadService;
    private final DownloadHistoryService downloadHistoryService;
    private final ShareLinkService shareLinkService;
    private final DownloadRequestContextFactory downloadRequestContextFactory;
    private final Day5ApiMapper day5ApiMapper;

    public GeneratedVersionController(
            GeneratedVersionQueryService generatedVersionQueryService,
            GeneratedVersionReviewService generatedVersionReviewService,
            DownloadService downloadService,
            DownloadHistoryService downloadHistoryService,
            ShareLinkService shareLinkService,
            DownloadRequestContextFactory downloadRequestContextFactory,
            Day5ApiMapper day5ApiMapper
    ) {
        this.generatedVersionQueryService = generatedVersionQueryService;
        this.generatedVersionReviewService = generatedVersionReviewService;
        this.downloadService = downloadService;
        this.downloadHistoryService = downloadHistoryService;
        this.shareLinkService = shareLinkService;
        this.downloadRequestContextFactory = downloadRequestContextFactory;
        this.day5ApiMapper = day5ApiMapper;
    }

    @GetMapping("/approvals/generated-versions")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    @Operation(summary = "List generated versions waiting for approval")
    public ApiResponse<List<GeneratedVersionResponse>> reviewQueue(@PathVariable UUID workspaceId) {
        return ApiResponse.success(day5ApiMapper.toGeneratedVersionResponses(
                generatedVersionReviewService.reviewQueue(workspaceId)));
    }

    @GetMapping("/creative-requests/{creativeRequestId}/generated-versions")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    @Operation(
            summary = "List generated versions for a creative request",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Generated versions returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GeneratedVersionResponse.class)))))
    public ApiResponse<List<GeneratedVersionResponse>> listGeneratedVersions(
            @PathVariable UUID workspaceId,
            @PathVariable UUID creativeRequestId
    ) {
        return ApiResponse.success(day5ApiMapper.toGeneratedVersionResponses(
                generatedVersionQueryService.listByCreativeRequest(workspaceId, creativeRequestId)));
    }

    @GetMapping("/generated-versions/{generatedVersionId}")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    @Operation(summary = "Get a generated version by id")
    public ApiResponse<GeneratedVersionResponse> getGeneratedVersion(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId
    ) {
        return ApiResponse.success(day5ApiMapper.toGeneratedVersionResponse(
                generatedVersionQueryService.getById(workspaceId, generatedVersionId)));
    }

    @PostMapping("/generated-versions/{generatedVersionId}/approve")
    @PreAuthorize("hasAuthority('GENERATED_VERSION_MANAGE')")
    @Operation(summary = "Approve a generated version")
    public ApiResponse<GeneratedVersionResponse> approve(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId,
            @Valid @RequestBody(required = false) GeneratedVersionApprovalDecisionRequest request
    ) {
        return ApiResponse.success(day5ApiMapper.toGeneratedVersionResponse(
                generatedVersionReviewService.approve(new GeneratedVersionApprovalActionCommand(
                        workspaceId,
                        generatedVersionId,
                        request == null ? null : request.comment()))));
    }

    @PostMapping("/generated-versions/{generatedVersionId}/reject")
    @PreAuthorize("hasAuthority('GENERATED_VERSION_MANAGE')")
    @Operation(summary = "Reject a generated version")
    public ApiResponse<GeneratedVersionResponse> reject(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId,
            @Valid @RequestBody GeneratedVersionApprovalDecisionRequest request
    ) {
        return ApiResponse.success(day5ApiMapper.toGeneratedVersionResponse(
                generatedVersionReviewService.reject(new GeneratedVersionApprovalActionCommand(
                        workspaceId,
                        generatedVersionId,
                        request == null ? null : request.comment()))));
    }

    @PostMapping("/generated-versions/{generatedVersionId}/request-changes")
    @PreAuthorize("hasAuthority('GENERATED_VERSION_MANAGE')")
    @Operation(summary = "Request changes for a generated version")
    public ApiResponse<GeneratedVersionResponse> requestChanges(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId,
            @Valid @RequestBody GeneratedVersionApprovalDecisionRequest request
    ) {
        return ApiResponse.success(day5ApiMapper.toGeneratedVersionResponse(
                generatedVersionReviewService.requestChanges(new GeneratedVersionApprovalActionCommand(
                        workspaceId,
                        generatedVersionId,
                        request == null ? null : request.comment()))));
    }

    @GetMapping("/generated-versions/{generatedVersionId}/approval-history")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    @Operation(summary = "List generated version approval history")
    public ApiResponse<List<GeneratedVersionApprovalHistoryView>> approvalHistory(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId
    ) {
        return ApiResponse.success(generatedVersionReviewService.history(workspaceId, generatedVersionId));
    }

    @PostMapping("/generated-versions/{generatedVersionId}/download-url")
    @PreAuthorize("hasAuthority('CREATIVE_DOWNLOAD')")
    @Operation(summary = "Generate a signed generated version download URL")
    public ApiResponse<GeneratedVersionPreviewUrlResponse> downloadUrl(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(day5ApiMapper.toGeneratedVersionPreviewUrlResponse(
                generatedVersionId,
                downloadService.requestGeneratedVersionDownload(
                        workspaceId,
                        generatedVersionId,
                        downloadRequestContextFactory.create(request, "generated-version-download"))));
    }

    @GetMapping("/generated-versions/{generatedVersionId}/downloads")
    @PreAuthorize("hasAuthority('CREATIVE_DOWNLOAD')")
    @Operation(summary = "List generated version download history")
    public ApiResponse<List<DownloadHistoryView>> downloads(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId
    ) {
        return ApiResponse.success(downloadHistoryService.listGeneratedVersionDownloadHistory(workspaceId, generatedVersionId));
    }

    @PostMapping("/generated-versions/{generatedVersionId}/share-links")
    @PreAuthorize("hasAuthority('CREATIVE_DOWNLOAD')")
    @Operation(summary = "Create a generated version share link")
    public ApiResponse<RevisedShareLinkView> createShareLink(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId,
            @Valid @RequestBody CreateRevisedShareLinkRequest request
    ) {
        return ApiResponse.success(shareLinkService.createRevisedShareLink(new CreateRevisedShareLinkCommand(
                workspaceId,
                generatedVersionId,
                request == null ? null : request.token(),
                request == null ? null : request.expiresAt())));
    }

    @GetMapping("/generated-versions/{generatedVersionId}/share-links")
    @PreAuthorize("hasAuthority('CREATIVE_DOWNLOAD')")
    @Operation(summary = "List generated version share links")
    public ApiResponse<List<RevisedShareLinkView>> shareLinks(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId
    ) {
        return ApiResponse.success(shareLinkService.listForGeneratedVersion(workspaceId, generatedVersionId));
    }

    @PostMapping("/generated-versions/{generatedVersionId}/share-links/{shareLinkId}/revoke")
    @PreAuthorize("hasAuthority('CREATIVE_DOWNLOAD')")
    @Operation(summary = "Revoke a generated version share link")
    public ApiResponse<RevisedShareLinkView> revokeShareLink(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId,
            @PathVariable UUID shareLinkId
    ) {
        return ApiResponse.success(shareLinkService.revoke(workspaceId, generatedVersionId, shareLinkId));
    }

    @GetMapping("/generated-versions/{generatedVersionId}/preview-url")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    @Operation(summary = "Generate a signed preview URL for a generated version")
    public ApiResponse<GeneratedVersionPreviewUrlResponse> previewUrl(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId
    ) {
        return ApiResponse.success(day5ApiMapper.toGeneratedVersionPreviewUrlResponse(
                generatedVersionId,
                generatedVersionQueryService.previewUrl(workspaceId, generatedVersionId)));
    }
}
