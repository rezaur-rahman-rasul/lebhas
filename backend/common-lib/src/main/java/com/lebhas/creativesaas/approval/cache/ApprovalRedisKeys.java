package com.lebhas.creativesaas.approval.cache;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ApprovalRedisKeys {

    public String approvalRequest(UUID approvalRequestId) {
        return "approval:request:" + approvalRequestId;
    }

    public String approvalWorkflow(UUID workflowId) {
        require(workflowId, "workflowId");
        return "approval:workflow:" + workflowId;
    }

    public String approvalState(UUID generatedVersionId) {
        require(generatedVersionId, "generatedVersionId");
        return "approval:state:" + generatedVersionId;
    }

    public String approvalStatus(UUID generatedVersionId) {
        return "approval:status:" + generatedVersionId;
    }

    public String approvalPending(UUID workspaceId) {
        return "approval:pending:" + workspaceId;
    }

    public String approvalReviewer(UUID reviewerId) {
        return "approval:reviewer:" + reviewerId;
    }

    public String reviewerAssignment(UUID workflowId) {
        require(workflowId, "workflowId");
        return "reviewer:assignment:" + workflowId;
    }

    public String shareLink(String token) {
        return "share:link:" + normalizeRequired(token, "token");
    }

    public String approvalLock(UUID approvalRequestId) {
        return "approval:lock:" + approvalRequestId;
    }

    public String approvalRevision(UUID generatedVersionId) {
        return "approval:revision:" + generatedVersionId;
    }

    private static void require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
