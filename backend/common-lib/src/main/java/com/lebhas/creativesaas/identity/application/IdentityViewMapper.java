package com.lebhas.creativesaas.identity.application;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.application.dto.UserView;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class IdentityViewMapper {

    public UserView toUserView(UserEntity user, UUID workspaceId, Role role, Set<Permission> permissions) {
        return new UserView(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                role,
                user.isMaster(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getLastLoginAt(),
                workspaceId,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                permissions);
    }
}
