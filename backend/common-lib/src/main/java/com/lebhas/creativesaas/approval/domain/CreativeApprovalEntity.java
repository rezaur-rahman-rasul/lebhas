package com.lebhas.creativesaas.approval.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legacy_creative_approvals", schema = "platform")
@Deprecated(forRemoval = true)
public class CreativeApprovalEntity extends TenantAwareEntity {

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "creative_output_id", nullable = false, updatable = false)
    private UUID creativeOutputId;

    @Column(name = "generation_request_id", nullable = false, updatable = false)
    private UUID generationRequestId;

    @Column(name = "submitted_by", nullable = false, updatable = false)
    private UUID submittedBy;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private CreativeApprovalStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private CreativeApprovalPriority priority;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "review_started_at")
    private Instant reviewStartedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "approval_note", length = 2000)
    private String approvalNote;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "regenerate_instruction", length = 2000)
    private String regenerateInstruction;

    protected CreativeApprovalEntity() {
    }

    public static CreativeApprovalEntity draft(
            UUID workspaceId,
            UUID projectId,
            UUID creativeOutputId,
            UUID generationRequestId,
            UUID submittedBy,
            CreativeApprovalPriority priority,
            Instant dueAt,
            String approvalNote
    ) {
        CreativeApprovalEntity approval = new CreativeApprovalEntity();
        approval.assignWorkspace(workspaceId);
        approval.projectId = requireProjectId(projectId);
        approval.creativeOutputId = creativeOutputId;
        approval.generationRequestId = generationRequestId;
        approval.submittedBy = submittedBy;
        approval.status = CreativeApprovalStatus.DRAFT;
        approval.priority = priority == null ? CreativeApprovalPriority.NORMAL : priority;
        approval.dueAt = dueAt;
        approval.approvalNote = normalizeNullable(approvalNote);
        return approval;
    }

    public UUID getCreativeOutputId() {
        return creativeOutputId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getGenerationRequestId() {
        return generationRequestId;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public CreativeApprovalStatus getStatus() {
        return status;
    }

    public CreativeApprovalPriority getPriority() {
        return priority;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getReviewStartedAt() {
        return reviewStartedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public String getApprovalNote() {
        return approvalNote;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getRegenerateInstruction() {
        return regenerateInstruction;
    }

    public void submit() {
        this.status = CreativeApprovalStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.reviewedBy = null;
        this.reviewStartedAt = null;
        this.reviewedAt = null;
    }

    public void startReview(UUID reviewerId) {
        this.status = CreativeApprovalStatus.IN_REVIEW;
        this.reviewedBy = reviewerId;
        this.reviewStartedAt = Instant.now();
    }

    public void approve(UUID reviewerId, String note) {
        this.status = CreativeApprovalStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.approvalNote = normalizeNullable(note);
        this.rejectionReason = null;
        this.regenerateInstruction = null;
    }

    public void reject(UUID reviewerId, String reason) {
        this.status = CreativeApprovalStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.rejectionReason = normalizeNullable(reason);
        this.approvalNote = null;
        this.regenerateInstruction = null;
    }

    public void requestRegenerate(UUID reviewerId, String instruction) {
        this.status = CreativeApprovalStatus.REGENERATE_REQUESTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.regenerateInstruction = normalizeNullable(instruction);
        this.approvalNote = null;
        this.rejectionReason = null;
    }

    public void cancel() {
        this.status = CreativeApprovalStatus.CANCELLED;
        this.reviewedAt = Instant.now();
    }

    public boolean isTerminal() {
        return status == CreativeApprovalStatus.APPROVED
                || status == CreativeApprovalStatus.REJECTED
                || status == CreativeApprovalStatus.CANCELLED;
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
