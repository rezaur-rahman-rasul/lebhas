package com.lebhas.creativesaas.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    protected void assignWorkspace(UUID workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        this.workspaceId = workspaceId;
    }
}
