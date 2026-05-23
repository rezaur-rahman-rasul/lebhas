package com.lebhas.creativesaas.common.tenant;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> WORKSPACE_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setWorkspaceId(UUID workspaceId) {
        WORKSPACE_ID.set(workspaceId);
    }

    public static Optional<UUID> getWorkspaceId() {
        return Optional.ofNullable(WORKSPACE_ID.get());
    }

    public static UUID requireWorkspaceId() {
        return getWorkspaceId().orElseThrow(() -> new BusinessException(
                ErrorCode.VALIDATION_FAILED,
                "Workspace context is required"));
    }

    public static void clear() {
        WORKSPACE_ID.remove();
    }
}
