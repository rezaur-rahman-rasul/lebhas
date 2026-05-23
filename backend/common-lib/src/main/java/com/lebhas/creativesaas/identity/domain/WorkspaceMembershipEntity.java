package com.lebhas.creativesaas.identity.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "workspace_memberships",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_workspace_memberships_workspace_user", columnNames = {"workspace_id", "user_id"})
)
public class WorkspaceMembershipEntity extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkspaceMembershipStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "workspace_membership_permissions",
            schema = "platform",
            joinColumns = @JoinColumn(name = "membership_id", nullable = false)
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_code", nullable = false, length = 60)
    private Set<Permission> permissions = new LinkedHashSet<>();

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "can_download_creative", nullable = false)
    private boolean canDownloadCreative;

    @Column(name = "can_edit_creative", nullable = false)
    private boolean canEditCreative;

    @Column(name = "invited_by_user_id")
    private UUID invitedByUserId;

    protected WorkspaceMembershipEntity() {
    }

    public static WorkspaceMembershipEntity create(
            UUID workspaceId,
            UUID userId,
            Role role,
            WorkspaceMembershipStatus status,
            Set<Permission> permissions,
            Instant joinedAt,
            UUID invitedByUserId
    ) {
        return create(
                workspaceId,
                userId,
                role,
                status,
                permissions,
                false,
                false,
                joinedAt,
                invitedByUserId);
    }

    public static WorkspaceMembershipEntity create(
            UUID workspaceId,
            UUID userId,
            Role role,
            WorkspaceMembershipStatus status,
            Set<Permission> permissions,
            boolean canDownloadCreative,
            boolean canEditCreative,
            Instant joinedAt,
            UUID invitedByUserId
    ) {
        WorkspaceMembershipEntity membership = new WorkspaceMembershipEntity();
        membership.assignWorkspace(workspaceId);
        membership.userId = userId;
        membership.role = role;
        membership.status = status;
        membership.permissions = new LinkedHashSet<>(permissions == null ? Set.of() : permissions);
        membership.joinedAt = joinedAt;
        membership.canDownloadCreative = allowsDownload(role, membership.permissions, canDownloadCreative);
        membership.canEditCreative = allowsEdit(role, membership.permissions, canEditCreative);
        membership.invitedByUserId = invitedByUserId;
        return membership;
    }

    public UUID getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public WorkspaceMembershipStatus getStatus() {
        return status;
    }

    public Set<Permission> getPermissions() {
        return Set.copyOf(permissions);
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public boolean canDownloadCreative() {
        return canDownloadCreative;
    }

    public boolean canEditCreative() {
        return canEditCreative;
    }

    public UUID getInvitedByUserId() {
        return invitedByUserId;
    }

    public boolean isActive() {
        return status == WorkspaceMembershipStatus.ACTIVE;
    }

    public void assignRole(Role role) {
        this.role = role;
        this.canDownloadCreative = allowsDownload(role, permissions, canDownloadCreative);
        this.canEditCreative = allowsEdit(role, permissions, canEditCreative);
    }

    public void replacePermissions(Set<Permission> permissions) {
        this.permissions.clear();
        this.permissions.addAll(permissions == null ? Set.of() : permissions);
        this.canDownloadCreative = allowsDownload(role, this.permissions, canDownloadCreative);
        this.canEditCreative = allowsEdit(role, this.permissions, canEditCreative);
    }

    public void updateCreativeAccess(boolean canDownloadCreative, boolean canEditCreative) {
        this.canDownloadCreative = allowsDownload(role, permissions, canDownloadCreative);
        this.canEditCreative = allowsEdit(role, permissions, canEditCreative);
    }

    public void changeStatus(WorkspaceMembershipStatus status) {
        this.status = status;
    }

    private static boolean allowsDownload(Role role, Set<Permission> permissions, boolean requestedValue) {
        return role == Role.MASTER
                || role == Role.ADMIN
                || requestedValue
                || permissions.contains(Permission.CREATIVE_DOWNLOAD);
    }

    private static boolean allowsEdit(Role role, Set<Permission> permissions, boolean requestedValue) {
        return role == Role.MASTER
                || role == Role.ADMIN
                || requestedValue
                || permissions.contains(Permission.CREATIVE_EDIT);
    }
}
