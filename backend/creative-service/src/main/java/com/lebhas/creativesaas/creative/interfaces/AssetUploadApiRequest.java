package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.asset.domain.AssetCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Schema(description = "Uploads a Day 4 asset using workspace context from X-Workspace-ID or the authenticated session.")
public class AssetUploadApiRequest {

    @NotNull
    @Schema(description = "Project or campaign identifier that owns the asset", format = "uuid")
    private UUID projectId;

    @NotNull
    @Schema(description = "Asset binary content", type = "string", format = "binary")
    private MultipartFile file;

    @NotNull
    @Schema(description = "High-level asset category")
    private AssetCategory assetCategory;

    @Size(max = 255)
    @Schema(description = "Optional display name shown in the UI", maxLength = 255)
    private String displayName;

    @Size(max = 2000)
    @Schema(description = "Optional asset description", maxLength = 2000)
    private String description;

    @Schema(description = "Optional comma-separated tag list")
    private String tags;

    @Schema(description = "Optional JSON object encoded as a string")
    private String metadata;

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public AssetCategory getAssetCategory() {
        return assetCategory;
    }

    public void setAssetCategory(AssetCategory assetCategory) {
        this.assetCategory = assetCategory;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
