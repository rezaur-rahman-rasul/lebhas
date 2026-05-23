package com.lebhas.creativesaas.common.security.context;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.AuthenticatedPrincipal;
import com.lebhas.creativesaas.common.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CurrentUserContext {

    public Optional<CurrentUser> getCurrentUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(AuthenticatedPrincipal.class::isInstance)
                .map(AuthenticatedPrincipal.class::cast)
                .map(this::toCurrentUser);
    }

    public CurrentUser requireCurrentUser() {
        return getCurrentUser().orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    public UUID requireWorkspaceId() {
        CurrentUser currentUser = requireCurrentUser();
        return TenantContext.getWorkspaceId()
                .or(() -> Optional.ofNullable(currentUser.workspaceId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_CONTEXT_REQUIRED));
    }

    private CurrentUser toCurrentUser(AuthenticatedPrincipal principal) {
        return new CurrentUser(
                principal.userId(),
                principal.workspaceId(),
                principal.deviceId(),
                principal.email(),
                principal.roles(),
                principal.permissions(),
                principal.tokenId(),
                principal.expiresAt());
    }
}
