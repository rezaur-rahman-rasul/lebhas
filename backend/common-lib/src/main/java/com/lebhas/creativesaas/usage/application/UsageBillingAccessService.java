package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UsageBillingAccessService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public UsageBillingAccessService(WorkspaceAuthorizationService workspaceAuthorizationService) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @Transactional(readOnly = true)
    public UUID requireUsageBillingView(UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        if (access.effectiveRole().isMaster() || access.permissions().contains(Permission.WORKSPACE_SETTINGS_VIEW)) {
            return access.workspace().getId();
        }
        throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED);
    }
}
