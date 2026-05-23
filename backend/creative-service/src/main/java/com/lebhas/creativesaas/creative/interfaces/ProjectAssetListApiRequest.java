package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetStatus;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.domain.PreviewStatus;
import com.lebhas.creativesaas.asset.domain.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Filters project assets within the resolved workspace context.")
public class ProjectAssetListApiRequest {

    private AssetType assetType;
    private AssetCategory assetCategory;
    private PreviewStatus previewStatus;
    private ProcessingStatus processingStatus;
    private UUID uploadedBy;
    private AssetStatus status;

    @Schema(description = "Keyword search across asset fields")
    private String search;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Created-at lower bound in ISO-8601 format")
    private Instant createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Created-at upper bound in ISO-8601 format")
    private Instant createdTo;

    @Min(value = 0, message = "Page index must be zero or greater")
    @Schema(description = "Zero-based page index", defaultValue = "0")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    @Schema(description = "Page size", defaultValue = "20", minimum = "1", maximum = "100")
    private Integer size = 20;

    @Schema(description = "Sort field", defaultValue = "createdAt")
    private String sortBy = "createdAt";

    @Schema(description = "Sort direction", defaultValue = "DESC")
    private Sort.Direction direction = Sort.Direction.DESC;

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public AssetCategory getAssetCategory() {
        return assetCategory;
    }

    public void setAssetCategory(AssetCategory assetCategory) {
        this.assetCategory = assetCategory;
    }

    public PreviewStatus getPreviewStatus() {
        return previewStatus;
    }

    public void setPreviewStatus(PreviewStatus previewStatus) {
        this.previewStatus = previewStatus;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UUID uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Instant getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(Instant createdFrom) {
        this.createdFrom = createdFrom;
    }

    public Instant getCreatedTo() {
        return createdTo;
    }

    public void setCreatedTo(Instant createdTo) {
        this.createdTo = createdTo;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public Sort.Direction getDirection() {
        return direction;
    }

    public void setDirection(Sort.Direction direction) {
        this.direction = direction;
    }
}
