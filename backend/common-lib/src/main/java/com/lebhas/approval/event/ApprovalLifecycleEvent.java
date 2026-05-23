package com.lebhas.approval.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ApprovalLifecycleEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID approvalRequestId,
        UUID generatedVersionId,
        UUID projectCampaignId,
        UUID submittedBy,
        UUID actorId,
        UUID assignedReviewerId,
        UUID previousAssignedReviewerId,
        ApprovalStatus previousStatus,
        ApprovalStatus currentStatus,
        Instant dueAt,
        String details,
        boolean internalOnly,
        int revisionCount
) {

    public ApprovalLifecycleEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        approvalRequestId = Objects.requireNonNull(approvalRequestId, "approvalRequestId must not be null");
        generatedVersionId = Objects.requireNonNull(generatedVersionId, "generatedVersionId must not be null");
        projectCampaignId = Objects.requireNonNull(projectCampaignId, "projectCampaignId must not be null");
        submittedBy = Objects.requireNonNull(submittedBy, "submittedBy must not be null");
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        currentStatus = currentStatus == null ? ApprovalStatus.NOT_SUBMITTED : currentStatus;
        details = normalizeNullable(details);
        revisionCount = Math.max(revisionCount, 0);
    }

    public static ApprovalLifecycleEvent from(
            ApprovalRequest request,
            UUID actorId,
            ApprovalStatus previousStatus,
            ApprovalStatus currentStatus,
            String details
    ) {
        return from(request, actorId, previousStatus, currentStatus, details, false, null);
    }

    public static ApprovalLifecycleEvent from(
            ApprovalRequest request,
            UUID actorId,
            ApprovalStatus previousStatus,
            ApprovalStatus currentStatus,
            String details,
            boolean internalOnly,
            UUID previousAssignedReviewerId
    ) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(request.getId(), "approvalRequestId must not be null");
        return new ApprovalLifecycleEvent(
                null,
                null,
                request.getWorkspaceId(),
                request.getId(),
                request.getGeneratedVersionId(),
                request.getProjectCampaignId(),
                request.getSubmittedBy(),
                actorId,
                request.getAssignedReviewerId(),
                previousAssignedReviewerId,
                previousStatus,
                currentStatus,
                request.getDueAt(),
                details,
                internalOnly,
                request.getRevisionCount());
    }

    @JsonIgnore
    public boolean isReassignment() {
        return previousAssignedReviewerId != null && !Objects.equals(previousAssignedReviewerId, assignedReviewerId);
    }

    private static String normalizeEventId(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
