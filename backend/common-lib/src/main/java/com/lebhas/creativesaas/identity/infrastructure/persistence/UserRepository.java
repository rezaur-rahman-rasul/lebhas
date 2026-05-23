package com.lebhas.creativesaas.identity.infrastructure.persistence;

import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailIgnoreCaseAndDeletedFalse(String email);

    Optional<UserEntity> findByIdAndDeletedFalse(UUID id);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    List<UserEntity> findAllByIdInAndDeletedFalse(Iterable<UUID> ids);

    @Query("""
            select distinct u
            from UserEntity u
            join WorkspaceMembershipEntity membership on membership.userId = u.id
            where u.deleted = false
              and membership.deleted = false
              and membership.workspaceId = :workspaceId
              and membership.status = com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus.ACTIVE
              and (:status is null or u.status = :status)
            order by u.createdAt desc
            """)
    List<UserEntity> findWorkspaceUsers(@Param("workspaceId") UUID workspaceId, @Param("status") UserStatus status);

    @Query("""
            select distinct u
            from UserEntity u
            join WorkspaceMembershipEntity membership on membership.userId = u.id
            where u.deleted = false
              and membership.deleted = false
              and membership.workspaceId = :workspaceId
              and membership.userId = :userId
              and membership.status = com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus.ACTIVE
            """)
    Optional<UserEntity> findWorkspaceUserById(@Param("workspaceId") UUID workspaceId, @Param("userId") UUID userId);
}
