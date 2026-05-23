package com.lebhas.creativesaas.generatedversion.domain;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generated_versions", schema = "platform")
public class GeneratedVersionEntity extends TenantAwareEntity {

    @Column(name = "creative_request_id", nullable = false, updatable = false)
    private UUID creativeRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creative_request_id", nullable = false, insertable = false, updatable = false)
    private CreativeRequestEntity creativeRequest;

    @Column(name = "generation_provider", length = 120)
    private String generationProvider;

    @Column(name = "generation_model", length = 160)
    private String generationModel;

    @Column(name = "prompt_snapshot", columnDefinition = "TEXT")
    private String promptSnapshot;

    @Column(name = "generated_asset_id")
    private UUID generatedAssetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_asset_id", insertable = false, updatable = false)
    private AssetEntity generatedAsset;

    @Column(name = "preview_asset_id")
    private UUID previewAssetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preview_asset_id", insertable = false, updatable = false)
    private AssetEntity previewAsset;

    @Column(name = "thumbnail_asset_id")
    private UUID thumbnailAssetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_asset_id", insertable = false, updatable = false)
    private AssetEntity thumbnailAsset;

    @Column(name = "generation_duration_ms")
    private Long generationDurationMs;

    @Column(name = "generation_cost_credits", precision = 19, scale = 4)
    private BigDecimal generationCostCredits;

    @Column(name = "generation_cost_usd", precision = 19, scale = 6)
    private BigDecimal generationCostUsd;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "format", length = 40)
    private String format;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "project_campaign_id", updatable = false)
    private UUID projectCampaignId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "version_name", length = 180)
    private String versionName;

    @Column(name = "storage_file_id")
    private UUID storageFileId;

    @Column(name = "asset_id")
    private UUID assetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 30)
    private GenerationStatus generationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    private ApprovalStatus approvalStatus;

    @Column(name = "submitted_for_approval_at")
    private Instant submittedForApprovalAt;

    @Column(name = "latest_approval_comment", length = 2000)
    private String latestApprovalComment;

    @Column(name = "latest_reviewer_id")
    private UUID latestReviewerId;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "approval_completed_at")
    private Instant approvalCompletedAt;

    @Column(name = "editable_before_approval", nullable = false)
    private boolean editableBeforeApproval;

    @Column(name = "generated_by_provider", length = 120)
    private String generatedByProvider;

    @Column(name = "generated_by_model", length = 160)
    private String generatedByModel;

    @Column(name = "created_by_user_id", updatable = false)
    private UUID createdByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GeneratedVersionStatus status;

    protected GeneratedVersionEntity() {
    }

    public static GeneratedVersionEntity create(
            UUID workspaceId,
            UUID creativeRequestId,
            int versionNumber,
            String generationProvider,
            String generationModel,
            String promptSnapshot,
            UUID generatedAssetId,
            UUID previewAssetId,
            UUID thumbnailAssetId,
            GenerationStatus generationStatus,
            Long generationDurationMs,
            BigDecimal generationCostCredits,
            BigDecimal generationCostUsd,
            Integer width,
            Integer height,
            String format,
            int retryCount,
            String failureReason
    ) {
        GeneratedVersionEntity entity = new GeneratedVersionEntity();
        entity.assignWorkspace(workspaceId);
        entity.creativeRequestId = require(creativeRequestId, "creativeRequestId");
        entity.versionNumber = normalizeVersionNumber(versionNumber);
        entity.generationProvider = normalizeNullable(generationProvider);
        entity.generationModel = normalizeNullable(generationModel);
        entity.promptSnapshot = normalizeNullable(promptSnapshot);
        entity.generatedAssetId = generatedAssetId;
        entity.assetId = generatedAssetId;
        entity.previewAssetId = previewAssetId;
        entity.thumbnailAssetId = thumbnailAssetId;
        entity.generationStatus = generationStatus == null ? GenerationStatus.QUEUED : generationStatus;
        entity.approvalStatus = ApprovalStatus.NOT_SUBMITTED;
        entity.editableBeforeApproval = true;
        entity.revisionNumber = 0;
        entity.generationDurationMs = normalizeNonNegative(generationDurationMs, "generationDurationMs");
        entity.generationCostCredits = normalizeMoney(generationCostCredits);
        entity.generationCostUsd = normalizeMoney(generationCostUsd);
        entity.width = normalizePositiveNullable(width, "width");
        entity.height = normalizePositiveNullable(height, "height");
        entity.format = normalizeNullable(format);
        entity.retryCount = normalizeRetryCount(retryCount);
        entity.failureReason = normalizeNullable(failureReason);
        entity.generatedByProvider = entity.generationProvider;
        entity.generatedByModel = entity.generationModel;
        entity.status = GeneratedVersionStatus.ACTIVE;
        return entity;
    }

    public static GeneratedVersionEntity create(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID projectCampaignId,
            int versionNumber,
            String versionName,
            UUID storageFileId,
            UUID assetId,
            String generatedByProvider,
            String generatedByModel,
            UUID createdByUserId
    ) {
        return create(
                workspaceId,
                creativeRequestId,
                projectCampaignId,
                versionNumber,
                versionName,
                storageFileId,
                assetId,
                GenerationStatus.QUEUED,
                ApprovalStatus.NOT_SUBMITTED,
                true,
                generatedByProvider,
                generatedByModel,
                createdByUserId,
                GeneratedVersionStatus.ACTIVE);
    }

    public static GeneratedVersionEntity create(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID projectCampaignId,
            int versionNumber,
            String versionName,
            UUID storageFileId,
            UUID assetId,
            GenerationStatus generationStatus,
            ApprovalStatus approvalStatus,
            boolean editableBeforeApproval,
            String generatedByProvider,
            String generatedByModel,
            UUID createdByUserId,
            GeneratedVersionStatus status
    ) {
        GeneratedVersionEntity entity = new GeneratedVersionEntity();
        entity.assignWorkspace(workspaceId);
        entity.creativeRequestId = require(creativeRequestId, "creativeRequestId");
        entity.projectCampaignId = require(projectCampaignId, "projectCampaignId");
        entity.versionNumber = normalizeVersionNumber(versionNumber);
        entity.versionName = normalizeNullable(versionName);
        entity.storageFileId = storageFileId;
        entity.assetId = assetId;
        entity.generatedAssetId = assetId;
        entity.generationStatus = generationStatus == null ? GenerationStatus.QUEUED : generationStatus;
        entity.approvalStatus = approvalStatus == null ? ApprovalStatus.NOT_SUBMITTED : approvalStatus.canonical();
        entity.submittedForApprovalAt = null;
        entity.latestApprovalComment = null;
        entity.latestReviewerId = null;
        entity.revisionNumber = 0;
        entity.approvalCompletedAt = null;
        entity.editableBeforeApproval = editableBeforeApproval;
        entity.generatedByProvider = normalizeNullable(generatedByProvider);
        entity.generatedByModel = normalizeNullable(generatedByModel);
        entity.generationProvider = entity.generatedByProvider;
        entity.generationModel = entity.generatedByModel;
        entity.generationCostCredits = BigDecimal.ZERO;
        entity.generationCostUsd = BigDecimal.ZERO;
        entity.retryCount = 0;
        entity.createdByUserId = require(createdByUserId, "createdByUserId");
        entity.status = status == null ? GeneratedVersionStatus.ACTIVE : status;
        return entity;
    }

    public UUID getCreativeRequestId() {
        return creativeRequestId;
    }

    public CreativeRequestEntity getCreativeRequest() {
        return creativeRequest;
    }

    public String getGenerationProvider() {
        return generationProvider;
    }

    public String getGenerationModel() {
        return generationModel;
    }

    public String getPromptSnapshot() {
        return promptSnapshot;
    }

    public UUID getGeneratedAssetId() {
        return generatedAssetId;
    }

    public AssetEntity getGeneratedAsset() {
        return generatedAsset;
    }

    public UUID getPreviewAssetId() {
        return previewAssetId;
    }

    public AssetEntity getPreviewAsset() {
        return previewAsset;
    }

    public UUID getThumbnailAssetId() {
        return thumbnailAssetId;
    }

    public AssetEntity getThumbnailAsset() {
        return thumbnailAsset;
    }

    public Long getGenerationDurationMs() {
        return generationDurationMs;
    }

    public BigDecimal getGenerationCostCredits() {
        return generationCostCredits;
    }

    public BigDecimal getGenerationCostUsd() {
        return generationCostUsd;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public String getFormat() {
        return format;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public UUID getProjectCampaignId() {
        return projectCampaignId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getVersionName() {
        return versionName;
    }

    public UUID getStorageFileId() {
        return storageFileId;
    }

    public UUID getAssetId() {
        return generatedAssetId == null ? assetId : generatedAssetId;
    }

    public GenerationStatus getGenerationStatus() {
        return generationStatus;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public Instant getSubmittedForApprovalAt() {
        return submittedForApprovalAt;
    }

    public String getLatestApprovalComment() {
        return latestApprovalComment;
    }

    public UUID getLatestReviewerId() {
        return latestReviewerId;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public Instant getApprovalCompletedAt() {
        return approvalCompletedAt;
    }

    public boolean isEditableBeforeApproval() {
        return editableBeforeApproval;
    }

    public String getGeneratedByProvider() {
        return generationProvider == null ? generatedByProvider : generationProvider;
    }

    public String getGeneratedByModel() {
        return generationModel == null ? generatedByModel : generationModel;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public GeneratedVersionStatus getStatus() {
        return status;
    }

    public void markProcessing() {
        this.generationStatus = GenerationStatus.PROCESSING;
        this.failureReason = null;
    }

    public void markReady(UUID storageFileId, UUID assetId, String generatedByProvider, String generatedByModel) {
        this.storageFileId = storageFileId;
        this.assetId = assetId;
        this.generatedByProvider = normalizeNullable(generatedByProvider);
        this.generatedByModel = normalizeNullable(generatedByModel);
        this.generatedAssetId = assetId;
        this.generationProvider = this.generatedByProvider;
        this.generationModel = this.generatedByModel;
        this.failureReason = null;
        this.generationStatus = GenerationStatus.READY;
    }

    public void markFailed() {
        markFailed(null);
    }

    public void markFailed(String failureReason) {
        this.failureReason = normalizeNullable(failureReason);
        this.generationStatus = GenerationStatus.FAILED;
    }

    public void recordGeneratedAsset(
            UUID generatedAssetId,
            UUID previewAssetId,
            UUID thumbnailAssetId,
            Long generationDurationMs,
            BigDecimal generationCostCredits,
            BigDecimal generationCostUsd,
            Integer width,
            Integer height,
            String format
    ) {
        this.generatedAssetId = generatedAssetId;
        this.assetId = generatedAssetId;
        this.previewAssetId = previewAssetId;
        this.thumbnailAssetId = thumbnailAssetId;
        this.generationDurationMs = normalizeNonNegative(generationDurationMs, "generationDurationMs");
        this.generationCostCredits = normalizeMoney(generationCostCredits);
        this.generationCostUsd = normalizeMoney(generationCostUsd);
        this.width = normalizePositiveNullable(width, "width");
        this.height = normalizePositiveNullable(height, "height");
        this.format = normalizeNullable(format);
    }

    public void incrementRetryCount() {
        this.retryCount = this.retryCount + 1;
    }

    public void markSubmittedForApproval() {
        this.approvalStatus = ApprovalStatus.SUBMITTED;
        this.submittedForApprovalAt = Instant.now();
        this.approvalCompletedAt = null;
        this.editableBeforeApproval = false;
    }

    public void markInReview(UUID reviewerId, String latestApprovalComment) {
        this.approvalStatus = ApprovalStatus.IN_REVIEW;
        this.latestReviewerId = reviewerId;
        this.latestApprovalComment = normalizeNullable(latestApprovalComment);
        this.approvalCompletedAt = null;
        this.editableBeforeApproval = false;
    }

    public void markResubmitted(String latestApprovalComment) {
        this.approvalStatus = ApprovalStatus.RESUBMITTED;
        this.submittedForApprovalAt = Instant.now();
        this.latestApprovalComment = normalizeNullable(latestApprovalComment);
        this.approvalCompletedAt = null;
        this.revisionNumber = this.revisionNumber + 1;
        this.editableBeforeApproval = false;
    }

    public void markApproved() {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approvalCompletedAt = Instant.now();
        this.editableBeforeApproval = false;
    }

    public void markApproved(UUID reviewerId, String latestApprovalComment) {
        this.latestReviewerId = reviewerId;
        this.latestApprovalComment = normalizeNullable(latestApprovalComment);
        markApproved();
    }

    public void markRejected(boolean editableBeforeApproval) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.approvalCompletedAt = Instant.now();
        this.editableBeforeApproval = editableBeforeApproval;
    }

    public void markRejected(UUID reviewerId, String latestApprovalComment, boolean editableBeforeApproval) {
        this.latestReviewerId = reviewerId;
        this.latestApprovalComment = normalizeNullable(latestApprovalComment);
        markRejected(editableBeforeApproval);
    }

    public void markChangesRequested() {
        this.approvalStatus = ApprovalStatus.CHANGES_REQUESTED;
        this.approvalCompletedAt = null;
        this.editableBeforeApproval = true;
    }

    public void markChangesRequested(UUID reviewerId, String latestApprovalComment) {
        this.latestReviewerId = reviewerId;
        this.latestApprovalComment = normalizeNullable(latestApprovalComment);
        markChangesRequested();
    }

    public void recordApprovalComment(UUID reviewerId, String latestApprovalComment) {
        if (reviewerId != null) {
            this.latestReviewerId = reviewerId;
        }
        this.latestApprovalComment = normalizeNullable(latestApprovalComment);
    }

    public void assignReviewer(UUID reviewerId) {
        this.latestReviewerId = reviewerId;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static int normalizeVersionNumber(int versionNumber) {
        if (versionNumber < 1) {
            throw new IllegalArgumentException("versionNumber must be greater than zero");
        }
        return versionNumber;
    }

    private static int normalizeRetryCount(int retryCount) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must not be negative");
        }
        return retryCount;
    }

    private static Long normalizeNonNegative(Long value, String field) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    private static Integer normalizePositiveNullable(Integer value, String field) {
        if (value != null && value < 1) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }

    private static BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("cost values must not be negative");
        }
        return value;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
