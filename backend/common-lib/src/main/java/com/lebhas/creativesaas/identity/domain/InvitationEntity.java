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
        name = "invitations",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_invitations_token_id", columnNames = "token_id")
)
public class InvitationEntity extends TenantAwareEntity {

    @Column(name = "token_id", nullable = false, updatable = false)
    private UUID tokenId;

    @Column(name = "email", nullable = false, length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "invited_by_user_id", nullable = false, updatable = false)
    private UUID invitedByUserId;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "invitation_permissions",
            schema = "platform",
            joinColumns = @JoinColumn(name = "invitation_id", nullable = false)
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_code", nullable = false, length = 60)
    private Set<Permission> permissions = new LinkedHashSet<>();

    protected InvitationEntity() {
    }

    public static InvitationEntity create(
            UUID tokenId,
            UUID workspaceId,
            String email,
            Role role,
            UUID invitedByUserId,
            String tokenHash,
            Instant expiresAt,
            Set<Permission> permissions
    ) {
        InvitationEntity entity = new InvitationEntity();
        entity.tokenId = tokenId;
        entity.assignWorkspace(workspaceId);
        entity.email = email;
        entity.role = role;
        entity.invitedByUserId = invitedByUserId;
        entity.tokenHash = tokenHash;
        entity.expiresAt = expiresAt;
        entity.permissions = new LinkedHashSet<>(permissions == null ? Set.of() : permissions);
        return entity;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public UUID getInvitedByUserId() {
        return invitedByUserId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Set<Permission> getPermissions() {
        return Set.copyOf(permissions);
    }

    public InvitationStatus getStatus(Instant now) {
        if (revokedAt != null) {
            return InvitationStatus.REVOKED;
        }
        if (acceptedAt != null) {
            return InvitationStatus.ACCEPTED;
        }
        if (expiresAt.isBefore(now)) {
            return InvitationStatus.EXPIRED;
        }
        return InvitationStatus.PENDING;
    }

    public void markAccepted(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
