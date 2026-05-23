package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.asset.application.AssetFolderService;
import com.lebhas.creativesaas.asset.application.dto.AssetFolderView;
import com.lebhas.creativesaas.asset.application.dto.CreateAssetFolderCommand;
import com.lebhas.creativesaas.asset.application.dto.UpdateAssetFolderCommand;
import com.lebhas.creativesaas.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/asset-folders")
@Tag(name = "Asset Folders")
@SecurityRequirement(name = "bearerAuth")
public class AssetFolderController {

    private final AssetFolderService assetFolderService;

    public AssetFolderController(AssetFolderService assetFolderService) {
        this.assetFolderService = assetFolderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ASSET_FOLDER_MANAGE')")
    @Operation(summary = "Create an asset folder")
    public ApiResponse<AssetFolderView> createFolder(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateAssetFolderRequest request
    ) {
        return ApiResponse.success(assetFolderService.createFolder(new CreateAssetFolderCommand(
                workspaceId,
                request.name(),
                request.parentFolderId(),
                request.description())));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    @Operation(summary = "List asset folders for a workspace")
    public ApiResponse<List<AssetFolderView>> listFolders(@PathVariable UUID workspaceId) {
        return ApiResponse.success(assetFolderService.listFolders(workspaceId));
    }

    @PutMapping("/{folderId}")
    @PreAuthorize("hasAuthority('ASSET_FOLDER_MANAGE')")
    @Operation(summary = "Update an asset folder")
    public ApiResponse<AssetFolderView> updateFolder(
            @PathVariable UUID workspaceId,
            @PathVariable UUID folderId,
            @Valid @RequestBody UpdateAssetFolderRequest request
    ) {
        return ApiResponse.success(assetFolderService.updateFolder(new UpdateAssetFolderCommand(
                workspaceId,
                folderId,
                request.name(),
                request.parentFolderId(),
                request.description())));
    }

    @DeleteMapping("/{folderId}")
    @PreAuthorize("hasAuthority('ASSET_FOLDER_MANAGE')")
    @Operation(summary = "Soft delete an asset folder")
    public ApiResponse<Void> deleteFolder(@PathVariable UUID workspaceId, @PathVariable UUID folderId) {
        assetFolderService.deleteFolder(workspaceId, folderId);
        return ApiResponse.success("Asset folder deleted", null);
    }
}
