package com.lebhas.approval.dto;

import com.lebhas.creativesaas.approval.application.dto.ApprovalAssignmentView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalAuditLogView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalCommentView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalRequestListCriteria;
import com.lebhas.creativesaas.approval.application.dto.ApprovalRequestView;
import com.lebhas.creativesaas.approval.application.dto.ApprovalReviewView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ApprovalApiMapper {

    public ApprovalRequestListCriteria toCriteria(UUID workspaceId, ApprovalRequestFilterDto filter) {
        return new ApprovalRequestListCriteria(
                workspaceId,
                filter.getStatus(),
                filter.getReviewer(),
                filter.getSubmittedBy(),
                filter.getFromDate(),
                filter.getToDate(),
                Boolean.TRUE.equals(filter.getPendingOnly()),
                Boolean.TRUE.equals(filter.getApprovedOnly()));
    }

    public ApprovalRequestResponse toResponse(ApprovalRequestView view) {
        return new ApprovalRequestResponse(
                view.id(),
                view.workspaceId(),
                view.generatedVersionId(),
                view.projectCampaignId(),
                view.submittedBy(),
                view.assignedReviewerId(),
                view.currentStatus(),
                view.submittedAt(),
                view.reviewedAt(),
                view.dueAt(),
                view.latestComment(),
                view.revisionCount(),
                view.createdAt(),
                view.updatedAt());
    }

    public List<ApprovalRequestResponse> toRequestResponses(List<ApprovalRequestView> views) {
        return views.stream().map(this::toResponse).toList();
    }

    public ApprovalAssignmentResponse toResponse(ApprovalAssignmentView view) {
        return new ApprovalAssignmentResponse(
                view.id(),
                view.workspaceId(),
                view.approvalRequestId(),
                view.assignedTo(),
                view.assignedBy(),
                view.assignedAt(),
                view.assignmentStatus(),
                view.createdAt(),
                view.updatedAt());
    }

    public ApprovalReviewResponse toResponse(ApprovalReviewView view) {
        return new ApprovalReviewResponse(
                view.id(),
                view.workspaceId(),
                view.approvalRequestId(),
                view.reviewerId(),
                view.decision(),
                view.feedback(),
                view.reviewType(),
                view.reviewedAt(),
                view.createdAt());
    }

    public ApprovalAuditLogResponse toResponse(ApprovalAuditLogView view) {
        return new ApprovalAuditLogResponse(
                view.id(),
                view.workspaceId(),
                view.approvalRequestId(),
                view.generatedVersionId(),
                view.actorId(),
                view.action(),
                view.previousStatus(),
                view.newStatus(),
                view.details(),
                view.createdAt());
    }

    public ApprovalCommentResponse toResponse(ApprovalCommentView view) {
        return new ApprovalCommentResponse(
                view.id(),
                view.workspaceId(),
                view.approvalRequestId(),
                view.generatedVersionId(),
                view.commentedBy(),
                view.commentText(),
                view.internalOnly(),
                view.createdAt(),
                view.updatedAt());
    }

    public List<ApprovalCommentResponse> toCommentResponses(List<ApprovalCommentView> views) {
        return views.stream().map(this::toResponse).toList();
    }

    public ApprovalRequestDetailResponse toDetailResponse(
            ApprovalRequestView approvalRequest,
            List<ApprovalAssignmentView> assignments,
            List<ApprovalReviewView> reviews,
            List<ApprovalAuditLogView> auditLogs
    ) {
        return new ApprovalRequestDetailResponse(
                toResponse(approvalRequest),
                assignments.stream().map(this::toResponse).toList(),
                reviews.stream().map(this::toResponse).toList(),
                auditLogs.stream().map(this::toResponse).toList());
    }
}
