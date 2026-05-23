package com.lebhas.creativesaas.common.security.authorization;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class RolePermissionRegistry {

    private final Map<Role, Set<Permission>> permissionsByRole = new EnumMap<>(Role.class);

    public RolePermissionRegistry() {
        permissionsByRole.put(Role.MASTER, EnumSet.copyOf(Arrays.asList(Permission.values())));
        permissionsByRole.put(Role.ADMIN, EnumSet.of(
                Permission.USER_VIEW,
                Permission.USER_CREATE,
                Permission.USER_UPDATE,
                Permission.USER_STATUS_UPDATE,
                Permission.WORKSPACE_CREATE,
                Permission.WORKSPACE_VIEW,
                Permission.WORKSPACE_UPDATE,
                Permission.WORKSPACE_STATUS_UPDATE,
                Permission.WORKSPACE_SETTINGS_VIEW,
                Permission.WORKSPACE_SETTINGS_UPDATE,
                Permission.BRAND_VIEW,
                Permission.BRAND_PROFILE_UPDATE,
                Permission.BRAND_MANAGE,
                Permission.PRODUCT_VIEW,
                Permission.PRODUCT_MANAGE,
                Permission.PRODUCT_SERVICE_MANAGE,
                Permission.PROJECT_VIEW,
                Permission.PROJECT_CREATE,
                Permission.PROJECT_UPDATE,
                Permission.PROJECT_CAMPAIGN_MANAGE,
                Permission.CREATIVE_REQUEST_CREATE,
                Permission.CREATIVE_REQUEST_MANAGE,
                Permission.CREATIVE_VERSION_EDIT,
                Permission.GENERATED_VERSION_MANAGE,
                Permission.SUPPORT_WORKSPACE_ACCESS,
                Permission.CREW_VIEW,
                Permission.CREW_INVITE,
                Permission.CREATIVE_GENERATE,
                Permission.CREW_UPDATE,
                Permission.CREW_REMOVE,
                Permission.CREATIVE_EDIT,
                Permission.CREATIVE_DOWNLOAD,
                Permission.CREATIVE_SUBMIT,
                Permission.ASSET_VIEW,
                Permission.ASSET_UPLOAD,
                Permission.ASSET_UPDATE,
                Permission.ASSET_DELETE,
                Permission.ASSET_FOLDER_MANAGE,
                Permission.PROMPT_TEMPLATE_VIEW,
                Permission.PROMPT_TEMPLATE_MANAGE,
                Permission.PROMPT_INTELLIGENCE_USE,
                Permission.PROMPT_HISTORY_VIEW,
                Permission.SESSION_MANAGE));
        permissionsByRole.put(Role.CREW, EnumSet.noneOf(Permission.class));
    }

    public Set<Permission> resolve(Set<Role> roles) {
        EnumSet<Permission> resolved = EnumSet.noneOf(Permission.class);
        roles.forEach(role -> resolved.addAll(permissionsByRole.getOrDefault(role, Set.of())));
        return Set.copyOf(resolved);
    }

    public Set<Permission> resolve(Role role) {
        return Set.copyOf(permissionsByRole.getOrDefault(role, Set.of()));
    }
}
