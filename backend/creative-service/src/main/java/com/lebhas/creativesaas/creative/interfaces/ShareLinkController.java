package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.constants.CommonHeaders;
import com.lebhas.creativesaas.sharing.application.ShareLinkService;
import com.lebhas.creativesaas.sharing.application.dto.CreateRevisedShareLinkCommand;
import com.lebhas.creativesaas.sharing.application.dto.RevisedShareLinkView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/share-links")
@Tag(name = "Share Links")
@SecurityRequirement(name = "bearerAuth")
public class ShareLinkController {

    private final ShareLinkService shareLinkService;
    private final WorkspaceRequestResolver workspaceRequestResolver;

    public ShareLinkController(
            ShareLinkService shareLinkService,
            WorkspaceRequestResolver workspaceRequestResolver
    ) {
        this.shareLinkService = shareLinkService;
        this.workspaceRequestResolver = workspaceRequestResolver;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATIVE_DOWNLOAD')")
    @Operation(summary = "Create a share link")
    @Parameter(
            name = CommonHeaders.WORKSPACE_ID,
            in = ParameterIn.HEADER,
            required = false,
            description = "Workspace context. Optional when the access token is already bound to a workspace; required for master or cross-workspace access.",
            schema = @Schema(type = "string", format = "uuid"))
    public ApiResponse<RevisedShareLinkView> createShareLink(@Valid @RequestBody CreateRevisedShareLinkRequest request) {
        UUID workspaceId = workspaceRequestResolver.requireWorkspaceId();
        RevisedShareLinkView shareLink = shareLinkService.createRevisedShareLink(
                new CreateRevisedShareLinkCommand(
                        workspaceId,
                        request.generatedVersionId(),
                        request.token(),
                        request.expiresAt()));
        return ApiResponse.success("Share link created", shareLink);
    }

    @GetMapping("/{token}")
    @PreAuthorize("hasAnyAuthority('CREATIVE_DOWNLOAD','CREATIVE_SUBMIT','GENERATED_VERSION_MANAGE','SUPPORT_WORKSPACE_ACCESS')")
    @Operation(summary = "Get a share link by token")
    @Parameter(
            name = CommonHeaders.WORKSPACE_ID,
            in = ParameterIn.HEADER,
            required = false,
            description = "Workspace context. Optional when the access token is already bound to a workspace; required for master or cross-workspace access.",
            schema = @Schema(type = "string", format = "uuid"))
    public ApiResponse<RevisedShareLinkView> getShareLink(@PathVariable String token) {
        UUID workspaceId = workspaceRequestResolver.requireWorkspaceId();
        return ApiResponse.success(
                "Share link found",
                shareLinkService.getRevisedShareLinkByToken(workspaceId, token));
    }
}
