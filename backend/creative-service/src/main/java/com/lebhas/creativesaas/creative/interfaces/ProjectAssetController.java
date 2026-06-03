package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.asset.application.AssetManagementService;
import com.lebhas.creativesaas.asset.application.AssetMetadataSerializer;
import com.lebhas.creativesaas.asset.application.dto.AssetListCriteria;
import com.lebhas.creativesaas.asset.application.dto.AssetUploadUrlView;
import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.application.dto.CreateAssetUploadUrlCommand;
import com.lebhas.creativesaas.asset.application.dto.UploadAssetCommand;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.api.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/assets")
@Tag(name = "Project Assets")
@SecurityRequirement(name = "bearerAuth")
public class ProjectAssetController {

    private final AssetManagementService assetManagementService;
    private final AssetMetadataSerializer assetMetadataSerializer;

    public ProjectAssetController(
            AssetManagementService assetManagementService,
            AssetMetadataSerializer assetMetadataSerializer
    ) {
        this.assetManagementService = assetManagementService;
        this.assetMetadataSerializer = assetMetadataSerializer;
    }

    @PostMapping("/upload-url")
    @PreAuthorize("hasAuthority('ASSET_UPLOAD')")
    @Operation(summary = "Create a signed project asset upload URL")
    public ApiResponse<AssetUploadUrlView> createUploadUrl(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateAssetUploadUrlRequest request
    ) {
        return ApiResponse.success("Project asset upload URL created", assetManagementService.createUploadUrl(new CreateAssetUploadUrlCommand(
                workspaceId,
             projectId,
             request.assetType(),
             request.assetCategory(),
             request.folderId(),
             request.originalFileName(),
                request.contentType(),
                request.sizeBytes(),
                request.checksum(),
                request.displayName(),
                request.description(),
                request.tags(),
                request.metadata())));
    }

    @PostMapping(path = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('ASSET_UPLOAD')")
    @Operation(summary = "Upload a raw project asset")
    public ApiResponse<AssetView> uploadAsset(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid
            @ModelAttribute
            @RequestBody(content = @Content(mediaType = "multipart/form-data", schema = @Schema(implementation = UploadAssetRequest.class)))
            UploadAssetRequest request
    ) {
        return ApiResponse.success(assetManagementService.uploadAsset(new UploadAssetCommand(
                workspaceId,
                projectId,
                request.getAssetCategory(),
                request.getDisplayName(),
                request.getDescription(),
                parseTags(request.getTags()),
                parseMetadata(request.getMetadata()),
                request.getFile())));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    @Operation(summary = "List project assets")
    public ApiResponse<PagedResult<AssetView>> listAssets(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @ModelAttribute AssetListRequest request
    ) {
        return ApiResponse.success(assetManagementService.listAssets(new AssetListCriteria(
                workspaceId,
                projectId,
                request.getAssetType(),
                request.getAssetCategory(),
                request.getPreviewStatus(),
                request.getProcessingStatus(),
                request.getUploadedBy(),
                request.getStatus(),
                request.getSearch(),
                request.getCreatedFrom(),
                request.getCreatedTo(),
                request.getPage() == null ? 0 : request.getPage(),
                request.getSize() == null ? 20 : request.getSize(),
                request.getSortBy(),
                request.getDirection())));
    }

    private Set<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rawTags.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Map<String, Object> parseMetadata(String metadata) {
        return assetMetadataSerializer.deserialize(metadata);
    }
}
