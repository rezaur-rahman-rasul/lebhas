package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.asset.application.AssetManagementService;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.common.constants.CommonHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets")
@SecurityRequirement(name = "bearerAuth")
public class Day4AssetApiController {

    private final AssetManagementService assetManagementService;
    private final WorkspaceRequestResolver workspaceRequestResolver;
    private final Day4ApiMapper day4ApiMapper;

    public Day4AssetApiController(
            AssetManagementService assetManagementService,
            WorkspaceRequestResolver workspaceRequestResolver,
            Day4ApiMapper day4ApiMapper
    ) {
        this.assetManagementService = assetManagementService;
        this.workspaceRequestResolver = workspaceRequestResolver;
        this.day4ApiMapper = day4ApiMapper;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ASSET_UPLOAD')")
    @Operation(summary = "Upload an asset")
    @Parameter(
            name = CommonHeaders.WORKSPACE_ID,
            in = ParameterIn.HEADER,
            required = false,
            description = "Workspace context. Optional when the access token is already bound to a workspace; required for master or cross-workspace access.",
            schema = @Schema(type = "string", format = "uuid"))
    public ApiResponse<AssetResponse> uploadAsset(
            @Valid
            @ModelAttribute
            @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(implementation = AssetUploadApiRequest.class)))
            AssetUploadApiRequest request
    ) {
        UUID workspaceId = workspaceRequestResolver.requireWorkspaceId();
        return ApiResponse.success(
                "Asset uploaded",
                day4ApiMapper.toAssetResponse(
                        assetManagementService.uploadAsset(day4ApiMapper.toUploadAssetCommand(workspaceId, request))));
    }

    @GetMapping("/{assetId}")
    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    @Operation(summary = "Get a single asset")
    @Parameter(
            name = CommonHeaders.WORKSPACE_ID,
            in = ParameterIn.HEADER,
            required = false,
            description = "Workspace context. Optional when the access token is already bound to a workspace; required for master or cross-workspace access.",
            schema = @Schema(type = "string", format = "uuid"))
    public ApiResponse<AssetResponse> getAsset(@PathVariable UUID assetId) {
        UUID workspaceId = workspaceRequestResolver.requireWorkspaceId();
        return ApiResponse.success(day4ApiMapper.toAssetResponse(assetManagementService.getAsset(workspaceId, assetId)));
    }

    @DeleteMapping("/{assetId}")
    @PreAuthorize("hasAuthority('ASSET_DELETE')")
    @Operation(summary = "Soft delete an asset")
    @Parameter(
            name = CommonHeaders.WORKSPACE_ID,
            in = ParameterIn.HEADER,
            required = false,
            description = "Workspace context. Optional when the access token is already bound to a workspace; required for master or cross-workspace access.",
            schema = @Schema(type = "string", format = "uuid"))
    public ApiResponse<Void> deleteAsset(@PathVariable UUID assetId) {
        UUID workspaceId = workspaceRequestResolver.requireWorkspaceId();
        assetManagementService.deleteAsset(workspaceId, assetId);
        return ApiResponse.success("Asset deleted", null);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    @Operation(summary = "List project assets")
    @Parameter(
            name = CommonHeaders.WORKSPACE_ID,
            in = ParameterIn.HEADER,
            required = false,
            description = "Workspace context. Optional when the access token is already bound to a workspace; required for master or cross-workspace access.",
            schema = @Schema(type = "string", format = "uuid"))
    public ApiResponse<PagedResult<AssetResponse>> listAssetsByProject(
            @PathVariable UUID projectId,
            @Valid @ParameterObject @ModelAttribute ProjectAssetListApiRequest request
    ) {
        UUID workspaceId = workspaceRequestResolver.requireWorkspaceId();
        return ApiResponse.success(day4ApiMapper.toAssetPage(
                assetManagementService.listAssets(day4ApiMapper.toAssetListCriteria(workspaceId, projectId, request))));
    }
}
