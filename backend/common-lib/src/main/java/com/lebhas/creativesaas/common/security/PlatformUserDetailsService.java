package com.lebhas.creativesaas.common.security;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.authorization.RolePermissionRegistry;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class PlatformUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionRegistry rolePermissionRegistry;
    private final Clock clock;

    public PlatformUserDetailsService(
            UserRepository userRepository,
            RolePermissionRegistry rolePermissionRegistry,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.rolePermissionRegistry = rolePermissionRegistry;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmailIgnoreCaseAndDeletedFalse(username)
                .map(user -> PlatformUserDetails.from(user, rolePermissionRegistry, clock.instant()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
    }
}
