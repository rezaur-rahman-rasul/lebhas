package com.lebhas.creativesaas.workspace.application;

import com.lebhas.creativesaas.common.api.ApiError;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.authorization.RolePermissionRegistry;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class WorkspacePermissionPolicy {

    private static final Set<Permission> DEFAULT_CREW_PERMISSIONS = Set.of(
            Permission.WORKSPACE_VIEW,
            Permission.BRAND_VIEW,
            Permission.PRODUCT_VIEW,
            Permission.PROJECT_VIEW,
            Permission.ASSET_VIEW,
            Permission.CREATIVE_GENERATE);

    private static final Set<Permission> ASSIGNABLE_CREW_PERMISSIONS = Set.of(
            Permission.WORKSPACE_VIEW,
            Permission.BRAND_VIEW,
            Permission.PRODUCT_VIEW,
            Permission.PROJECT_VIEW,
            Permission.PROJECT_CREATE,
            Permission.PROJECT_UPDATE,
            Permission.ASSET_VIEW,
            Permission.ASSET_UPLOAD,
            Permission.PROMPT_TEMPLATE_VIEW,
            Permission.PROMPT_INTELLIGENCE_USE,
            Permission.PROMPT_HISTORY_VIEW,
            Permission.CREATIVE_GENERATE,
            Permission.CREATIVE_REQUEST_CREATE,
            Permission.CREATIVE_EDIT,
            Permission.CREATIVE_VERSION_EDIT,
            Permission.CREATIVE_DOWNLOAD,
            Permission.CREATIVE_SUBMIT);

    private final RolePermissionRegistry rolePermissionRegistry;

    public WorkspacePermissionPolicy(RolePermissionRegistry rolePermissionRegistry) {
        this.rolePermissionRegistry = rolePermissionRegistry;
    }

    public Set<Permission> resolveEffectivePermissions(Role role, Set<Permission> storedPermissions) {
        if (role == Role.MASTER || role == Role.ADMIN) {
            return rolePermissionRegistry.resolve(role);
        }
        Set<Permission> effectivePermissions = storedPermissions == null || storedPermissions.isEmpty()
                ? DEFAULT_CREW_PERMISSIONS
                : normalizeCrewPermissions(storedPermissions);
        return Set.copyOf(effectivePermissions);
    }

    public Set<Permission> normalizeCrewPermissions(Set<Permission> requestedPermissions) {
        EnumSet<Permission> normalized = requestedPermissions == null || requestedPermissions.isEmpty()
                ? EnumSet.copyOf(DEFAULT_CREW_PERMISSIONS)
                : EnumSet.copyOf(requestedPermissions);
        normalized.add(Permission.WORKSPACE_VIEW);
        normalized.add(Permission.BRAND_VIEW);
        normalized.add(Permission.PRODUCT_VIEW);
        normalized.add(Permission.PROJECT_VIEW);
        normalized.add(Permission.ASSET_VIEW);
        if (!ASSIGNABLE_CREW_PERMISSIONS.containsAll(normalized)) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_PERMISSION_INVALID,
                    "Crew permissions include unsupported values",
                    normalized.stream()
                            .filter(permission -> !ASSIGNABLE_CREW_PERMISSIONS.contains(permission))
                            .map(permission -> ApiError.of(
                                    ErrorCode.WORKSPACE_PERMISSION_INVALID.code(),
                                    "permissions",
                                    permission.name() + " cannot be assigned to crew members"))
                            .toList());
        }
        return Set.copyOf(normalized);
    }

    public Set<Permission> assignableCrewPermissions() {
        return ASSIGNABLE_CREW_PERMISSIONS;
    }
}
