package com.lebhas.creativesaas.operations.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "go_live_checklist_items", schema = "platform")
public class GoLiveChecklistItem extends BaseEntity {
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private GoLiveChecklistItemStatus status;

    @Column(name = "block_reason", length = 1000)
    private String blockReason;

    protected GoLiveChecklistItem() {
    }

    public static GoLiveChecklistItem create(String title, String description) {
        GoLiveChecklistItem item = new GoLiveChecklistItem();
        item.title = required(title);
        item.description = normalize(description);
        item.status = GoLiveChecklistItemStatus.PENDING;
        return item;
    }

    public void update(String title, String description) {
        this.title = required(title);
        this.description = normalize(description);
    }

    public void complete() {
        this.status = GoLiveChecklistItemStatus.COMPLETED;
        this.blockReason = null;
    }

    public void block(String reason) {
        this.status = GoLiveChecklistItemStatus.BLOCKED;
        this.blockReason = normalize(reason);
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public GoLiveChecklistItemStatus getStatus() { return status; }
    public String getBlockReason() { return blockReason; }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("title must not be blank");
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
