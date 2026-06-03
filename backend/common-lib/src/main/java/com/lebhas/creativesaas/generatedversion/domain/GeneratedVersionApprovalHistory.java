package com.lebhas.creativesaas.generatedversion.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "generated_version_approval_history", schema = "platform")
public class GeneratedVersionApprovalHistory extends TenantAwareEntity {

    @Column(name = "generated_version_id", nullable = false, updatable = false)
    private UUID generatedVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private GeneratedVersionApprovalAction action;

    @Column(name = "action_by", nullable = false, updatable = false)
    private UUID actionBy;

    @Column(name = "comment", length = 2000)
    private String comment;

    protected GeneratedVersionApprovalHistory() {
    }

    public static GeneratedVersionApprovalHistory record(
            UUID workspaceId,
            UUID generatedVersionId,
            GeneratedVersionApprovalAction action,
            UUID actionBy,
            String comment
    ) {
        GeneratedVersionApprovalHistory history = new GeneratedVersionApprovalHistory();
        history.assignWorkspace(require(workspaceId, "workspaceId"));
        history.generatedVersionId = require(generatedVersionId, "generatedVersionId");
        history.action = require(action, "action");
        history.actionBy = require(actionBy, "actionBy");
        history.comment = normalize(comment);
        return history;
    }

    public UUID getGeneratedVersionId() {
        return generatedVersionId;
    }

    public GeneratedVersionApprovalAction getAction() {
        return action;
    }

    public UUID getActionBy() {
        return actionBy;
    }

    public String getComment() {
        return comment;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
