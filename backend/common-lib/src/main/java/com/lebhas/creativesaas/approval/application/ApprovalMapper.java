package com.lebhas.creativesaas.approval.application;

import com.lebhas.creativesaas.approval.application.dto.ApprovalAssignmentView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalAuditLogView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalCommentView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalPendingSummaryView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalRequestView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalReviewView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalReviewerQueueView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalHistoryEntryView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalWorkflowView;
import com.lebhas.creativesaas.approval.cache.ApprovalPendingCacheEntry;
import com.lebhas.creativesaas.approval.cache.ApprovalRequestCacheEntry;
import com.lebhas.creativesaas.approval.cache.ApprovalReviewerCacheEntry;
import com.lebhas.creativesaas.approval.cache.ApprovalStatusCacheEntry;
import com.lebhas.creativesaas.approval.cache.ApprovalWorkflowCacheEntry;
import com.lebhas.creativesaas.approval.domain.ApprovalAssignment;
import com.lebhas.creativesaas.approval.domain.ApprovalAuditLog;
import com.lebhas.creativesaas.approval.domain.ApprovalComment;
import com.lebhas.creativesaas.approval.domain.ApprovalHistory;
import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.approval.domain.ApprovalReview;
import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.profile.application.SafeProfileDisplayService;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class ApprovalMapper {

    private SafeProfileDisplayService safeProfileDisplayService;

    @Autowired(required = false)
    public void setSafeProfileDisplayService(SafeProfileDisplayService safeProfileDisplayService) {
        this.safeProfileDisplayService = safeProfileDisplayService;
    }

    public ApprovalWorkflowView toView(ApprovalWorkflow entity) {
        return new ApprovalWorkflowView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getCreativeRequestId(),
                entity.getGeneratedVersionId(),
                entity.getCreatedBy(),
                entity.getCurrentStatus(),
                entity.getCurrentReviewerId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ApprovalWorkflowView toView(ApprovalWorkflowCacheEntry entry) {
        return new ApprovalWorkflowView(
                entry.workflowId(),
                entry.workspaceId(),
                entry.creativeRequestId(),
                entry.generatedVersionId(),
                entry.createdBy(),
                entry.currentStatus(),
                entry.currentReviewerId(),
                entry.createdAt(),
                entry.updatedAt());
    }

    public ApprovalHistoryEntryView toView(ApprovalHistory entity) {
        return new ApprovalHistoryEntryView(
                entity.getId(),
                entity.getApprovalWorkflowId(),
                entity.getActionBy(),
                entity.getActionType(),
                entity.getComments(),
                entity.getCreatedAt());
    }

    public ApprovalRequestView toView(ApprovalRequest entity) {
        return new ApprovalRequestView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getGeneratedVersionId(),
                entity.getProjectCampaignId(),
                entity.getSubmittedBy(),
                safeDisplay(entity.getWorkspaceId(), entity.getSubmittedBy()),
                entity.getAssignedReviewerId(),
                safeDisplay(entity.getWorkspaceId(), entity.getAssignedReviewerId()),
                entity.getCurrentStatus(),
                entity.getSubmittedAt(),
                entity.getReviewedAt(),
                entity.getDueAt(),
                entity.getLatestComment(),
                entity.getRevisionCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ApprovalRequestView toView(ApprovalRequestCacheEntry entry) {
        return new ApprovalRequestView(
                entry.approvalRequestId(),
                entry.workspaceId(),
                entry.generatedVersionId(),
                entry.projectCampaignId(),
                entry.submittedBy(),
                safeDisplay(entry.workspaceId(), entry.submittedBy()),
                entry.assignedReviewerId(),
                safeDisplay(entry.workspaceId(), entry.assignedReviewerId()),
                entry.currentStatus(),
                entry.submittedAt(),
                entry.reviewedAt(),
                entry.dueAt(),
                entry.latestComment(),
                entry.revisionCount(),
                entry.createdAt(),
                entry.updatedAt());
    }

    public ApprovalReviewView toView(ApprovalReview entity) {
        return new ApprovalReviewView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getApprovalRequestId(),
                entity.getReviewerId(),
                safeDisplay(entity.getWorkspaceId(), entity.getReviewerId()),
                entity.getDecision(),
                entity.getFeedback(),
                entity.getReviewType(),
                entity.getReviewedAt(),
                entity.getCreatedAt());
    }

    public ApprovalCommentView toView(ApprovalComment entity) {
        return new ApprovalCommentView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getApprovalRequestId(),
                entity.getGeneratedVersionId(),
                entity.getCommentedBy(),
                safeDisplay(entity.getWorkspaceId(), entity.getCommentedBy()),
                entity.getCommentText(),
                entity.isInternalOnly(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ApprovalAssignmentView toView(ApprovalAssignment entity) {
        return new ApprovalAssignmentView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getApprovalRequestId(),
                entity.getAssignedTo(),
                entity.getAssignedBy(),
                entity.getAssignedAt(),
                entity.getAssignmentStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ApprovalAuditLogView toView(ApprovalAuditLog entity) {
        return new ApprovalAuditLogView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getApprovalRequestId(),
                entity.getGeneratedVersionId(),
                entity.getActorId(),
                entity.getAction(),
                entity.getPreviousStatus(),
                entity.getNewStatus(),
                entity.getDetails(),
                entity.getCreatedAt());
    }

    public ApprovalRequestCacheEntry toRequestCacheEntry(ApprovalRequest entity) {
        return new ApprovalRequestCacheEntry(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getGeneratedVersionId(),
                entity.getProjectCampaignId(),
                entity.getSubmittedBy(),
                entity.getAssignedReviewerId(),
                entity.getCurrentStatus(),
                entity.getSubmittedAt(),
                entity.getReviewedAt(),
                entity.getDueAt(),
                entity.getLatestComment(),
                entity.getRevisionCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ApprovalStatusCacheEntry toStatusCacheEntry(ApprovalRequest request, GeneratedVersionEntity version) {
        return new ApprovalStatusCacheEntry(
                version.getId(),
                request.getId(),
                request.getWorkspaceId(),
                request.getCurrentStatus(),
                version.getLatestReviewerId(),
                version.getSubmittedForApprovalAt(),
                version.getApprovalCompletedAt(),
                version.getRevisionNumber(),
                version.getUpdatedAt());
    }

    public ApprovalPendingSummaryView toView(ApprovalPendingCacheEntry entry) {
        return new ApprovalPendingSummaryView(
                entry.workspaceId(),
                entry.pendingCount(),
                entry.inReviewCount(),
                entry.changesRequestedCount(),
                entry.pendingApprovalRequestIds(),
                entry.updatedAt());
    }

    public ApprovalPendingCacheEntry toPendingCacheEntry(
            UUID workspaceId,
            int pendingCount,
            int inReviewCount,
            int changesRequestedCount,
            List<UUID> pendingApprovalRequestIds
    ) {
        return new ApprovalPendingCacheEntry(
                workspaceId,
                pendingCount,
                inReviewCount,
                changesRequestedCount,
                pendingApprovalRequestIds,
                Instant.now());
    }

    public ApprovalReviewerQueueView toReviewerQueueView(
            UUID reviewerId,
            UUID workspaceId,
            int pendingCount,
            int inReviewCount,
            List<UUID> approvalRequestIds,
            Instant updatedAt
    ) {
        return new ApprovalReviewerQueueView(
                reviewerId,
                workspaceId,
                pendingCount,
                inReviewCount,
                approvalRequestIds,
                updatedAt);
    }

    public ApprovalReviewerQueueView toView(ApprovalReviewerCacheEntry entry, UUID workspaceId) {
        ApprovalReviewerCacheEntry.WorkspaceQueueState state = entry.workspaceQueues().stream()
                .filter(queue -> workspaceId.equals(queue.workspaceId()))
                .findFirst()
                .orElse(new ApprovalReviewerCacheEntry.WorkspaceQueueState(workspaceId, 0, 0, List.of()));
        return toReviewerQueueView(
                entry.reviewerId(),
                workspaceId,
                state.pendingCount(),
                state.inReviewCount(),
                state.approvalRequestIds(),
                entry.updatedAt());
    }

    public ApprovalReviewerCacheEntry toReviewerCacheEntry(
            UUID reviewerId,
            UUID workspaceId,
            int pendingCount,
            int inReviewCount,
            List<UUID> approvalRequestIds
    ) {
        return new ApprovalReviewerCacheEntry(
                reviewerId,
                List.of(new ApprovalReviewerCacheEntry.WorkspaceQueueState(
                        workspaceId,
                        pendingCount,
                        inReviewCount,
                        approvalRequestIds)),
                Instant.now());
    }

    private SafeProfileDisplayView safeDisplay(UUID workspaceId, UUID userId) {
        return safeProfileDisplayService == null ? null : safeProfileDisplayService.forUserInWorkspace(workspaceId, userId);
    }
}
