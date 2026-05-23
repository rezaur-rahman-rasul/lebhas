package com.lebhas.creativesaas.approval.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legacy_creative_approval_history", schema = "platform")
@Deprecated(forRemoval = true)
public class CreativeApprovalHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "approval_id", nullable = false, updatable = false)
    private UUID approvalId;

    @Column(name = "creative_output_id", nullable = false, updatable = false)
    private UUID creativeOutputId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private CreativeApprovalAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 40)
    private CreativeApprovalStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 40)
    private CreativeApprovalStatus newStatus;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CreativeApprovalHistoryEntity() {
    }

    public static CreativeApprovalHistoryEntity record(
            UUID workspaceId,
            UUID approvalId,
            UUID creativeOutputId,
            CreativeApprovalAction action,
            CreativeApprovalStatus previousStatus,
            CreativeApprovalStatus newStatus,
            UUID actorId,
            String note
    ) {
        CreativeApprovalHistoryEntity history = new CreativeApprovalHistoryEntity();
        history.workspaceId = workspaceId;
        history.approvalId = approvalId;
        history.creativeOutputId = creativeOutputId;
        history.action = action;
        history.previousStatus = previousStatus;
        history.newStatus = newStatus;
        history.actorId = actorId;
        history.note = normalizeNullable(note);
        history.createdAt = Instant.now();
        return history;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getApprovalId() {
        return approvalId;
    }

    public UUID getCreativeOutputId() {
        return creativeOutputId;
    }

    public CreativeApprovalAction getAction() {
        return action;
    }

    public CreativeApprovalStatus getPreviousStatus() {
        return previousStatus;
    }

    public CreativeApprovalStatus getNewStatus() {
        return newStatus;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
