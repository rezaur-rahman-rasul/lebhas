package com.lebhas.creativesaas.approval.application;

import com.lebhas.creativesaas.approval.application.dto.CreativeApprovalHistoryView;
import com.lebhas.creativesaas.approval.application.dto.CreativeApprovalView;
import com.lebhas.creativesaas.approval.application.dto.CreativeReviewCommentView;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalEntity;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalHistoryEntity;
import com.lebhas.creativesaas.approval.domain.CreativeReviewCommentEntity;
import org.springframework.stereotype.Component;

@Component
@Deprecated(forRemoval = true)
public class CreativeApprovalViewMapper {

    public CreativeApprovalView toApprovalView(CreativeApprovalEntity approval) {
        return new CreativeApprovalView(
                approval.getId(),
                approval.getWorkspaceId(),
                approval.getCreativeOutputId(),
                approval.getGenerationRequestId(),
                approval.getSubmittedBy(),
                approval.getReviewedBy(),
                approval.getStatus(),
                approval.getPriority(),
                approval.getSubmittedAt(),
                approval.getReviewStartedAt(),
                approval.getReviewedAt(),
                approval.getDueAt(),
                approval.getApprovalNote(),
                approval.getRejectionReason(),
                approval.getRegenerateInstruction(),
                approval.getCreatedAt(),
                approval.getUpdatedAt());
    }

    public CreativeReviewCommentView toCommentView(CreativeReviewCommentEntity comment) {
        return new CreativeReviewCommentView(
                comment.getId(),
                comment.getWorkspaceId(),
                comment.getApprovalId(),
                comment.getCreativeOutputId(),
                comment.getAuthorId(),
                comment.getComment(),
                comment.getCommentType(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }

    public CreativeApprovalHistoryView toHistoryView(CreativeApprovalHistoryEntity history) {
        return new CreativeApprovalHistoryView(
                history.getId(),
                history.getWorkspaceId(),
                history.getApprovalId(),
                history.getCreativeOutputId(),
                history.getAction(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getActorId(),
                history.getNote(),
                history.getCreatedAt());
    }
}
