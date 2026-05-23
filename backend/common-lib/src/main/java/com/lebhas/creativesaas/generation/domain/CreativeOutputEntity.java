package com.lebhas.creativesaas.generation.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "creative_outputs", schema = "platform")
public class CreativeOutputEntity extends TenantAwareEntity {

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "generated_asset_id")
    private UUID generatedAssetId;

    @Column(name = "storage_file_id")
    private UUID storageFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "creative_type", nullable = false, length = 40)
    private CreativeType creativeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 40)
    private PromptPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", nullable = false, length = 20)
    private CreativeOutputFormat outputFormat;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "preview_url", length = 1000)
    private String previewUrl;

    @Column(name = "download_url", length = 1000)
    private String downloadUrl;

    @Column(name = "caption", length = 500)
    private String caption;

    @Column(name = "headline", length = 240)
    private String headline;

    @Column(name = "cta_text", length = 120)
    private String ctaText;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CreativeGenerationStatus status;

    protected CreativeOutputEntity() {
    }

    public static CreativeOutputEntity processing(
            UUID workspaceId,
            UUID projectId,
            UUID requestId,
            CreativeType creativeType,
            PromptPlatform platform,
            CreativeOutputFormat outputFormat,
            Integer width,
            Integer height,
            Long duration
    ) {
        CreativeOutputEntity output = new CreativeOutputEntity();
        output.assignWorkspace(workspaceId);
        output.projectId = requireProjectId(projectId);
        output.requestId = requestId;
        output.creativeType = creativeType;
        output.platform = platform;
        output.outputFormat = outputFormat;
        output.width = width;
        output.height = height;
        output.duration = duration;
        output.status = CreativeGenerationStatus.PROCESSING;
        return output;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getGeneratedAssetId() {
        return generatedAssetId;
    }

    public UUID getStorageFileId() {
        return storageFileId;
    }

    public CreativeType getCreativeType() {
        return creativeType;
    }

    public PromptPlatform getPlatform() {
        return platform;
    }

    public CreativeOutputFormat getOutputFormat() {
        return outputFormat;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Long getDuration() {
        return duration;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getCaption() {
        return caption;
    }

    public String getHeadline() {
        return headline;
    }

    public String getCtaText() {
        return ctaText;
    }

    public String getMetadata() {
        return metadata;
    }

    public CreativeGenerationStatus getStatus() {
        return status;
    }

    public void complete(
            UUID generatedAssetId,
            UUID storageFileId,
            Integer width,
            Integer height,
            Long duration,
            Long fileSize,
            String previewUrl,
            String downloadUrl,
            String caption,
            String headline,
            String ctaText,
            String metadata
    ) {
        this.generatedAssetId = generatedAssetId;
        this.storageFileId = storageFileId;
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.fileSize = fileSize;
        this.previewUrl = normalizeNullable(previewUrl);
        this.downloadUrl = normalizeNullable(downloadUrl);
        this.caption = normalizeNullable(caption);
        this.headline = normalizeNullable(headline);
        this.ctaText = normalizeNullable(ctaText);
        this.metadata = normalizeNullable(metadata);
        this.status = CreativeGenerationStatus.COMPLETED;
    }

    public void markFailed(String metadata) {
        this.metadata = normalizeNullable(metadata);
        this.status = CreativeGenerationStatus.FAILED;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static UUID requireProjectId(UUID projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        return projectId;
    }
}
