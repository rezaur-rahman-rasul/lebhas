package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.asset.application.AssetManagementService;
import com.lebhas.creativesaas.asset.application.dto.AssetUrlView;
import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.application.dto.UpdateAssetCommand;
import com.lebhas.creativesaas.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/assets")
@Tag(name = "Assets")
@SecurityRequirement(name = "bearerAuth")
public class AssetController {

    private final AssetManagementService assetManagementService;

    public AssetController(AssetManagementService assetManagementService) {
        this.assetManagementService = assetManagementService;
    }

    @GetMapping("/{assetId}")
    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    @Operation(summary = "Get asset metadata")
    public ApiResponse<AssetView> getAsset(@PathVariable UUID workspaceId, @PathVariable UUID assetId) {
        return ApiResponse.success(assetManagementService.getAsset(workspaceId, assetId));
    }

    @PutMapping("/{assetId}")
    @PreAuthorize("hasAuthority('ASSET_UPDATE')")
    @Operation(summary = "Update asset metadata")
    public ApiResponse<AssetView> updateAsset(
            @PathVariable UUID workspaceId,
            @PathVariable UUID assetId,
            @RequestBody UpdateAssetRequest request
    ) {
        return ApiResponse.success(assetManagementService.updateAsset(new UpdateAssetCommand(
                workspaceId,
                assetId,
                request.displayName(),
                request.description(),
                request.assetCategory(),
                request.tags(),
                request.metadata())));
    }

    @DeleteMapping("/{assetId}")
    @PreAuthorize("hasAuthority('ASSET_DELETE')")
    @Operation(summary = "Soft delete an asset")
    public ApiResponse<Void> deleteAsset(@PathVariable UUID workspaceId, @PathVariable UUID assetId) {
        assetManagementService.deleteAsset(workspaceId, assetId);
        return ApiResponse.success("Asset deleted", null);
    }

    @GetMapping("/{assetId}/preview-url")
    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    @Operation(summary = "Generate a signed preview URL")
    public ApiResponse<AssetUrlView> previewUrl(@PathVariable UUID workspaceId, @PathVariable UUID assetId) {
        return ApiResponse.success(assetManagementService.generatePreviewUrl(workspaceId, assetId));
    }

    @GetMapping("/{assetId}/download-url")
    @PreAuthorize("hasAuthority('CREATIVE_DOWNLOAD')")
    @Operation(summary = "Generate a signed download URL")
    public ApiResponse<AssetUrlView> downloadUrl(@PathVariable UUID workspaceId, @PathVariable UUID assetId) {
        return ApiResponse.success(assetManagementService.generateDownloadUrl(workspaceId, assetId));
    }
}
