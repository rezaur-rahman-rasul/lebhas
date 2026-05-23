package com.lebhas.creativesaas.common.security;

import com.lebhas.creativesaas.common.security.authorization.RolePermissionRegistry;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class PlatformUserDetails implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String password;
    private final Role role;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Set<GrantedAuthority> authorities;

    private PlatformUserDetails(
            UUID userId,
            String email,
            String password,
            Role role,
            boolean enabled,
            boolean accountNonLocked,
            Set<GrantedAuthority> authorities
    ) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
    }

    public static PlatformUserDetails from(
            UserEntity user,
            RolePermissionRegistry rolePermissionRegistry,
            java.time.Instant now
    ) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority(AuthorityNames.role(user.getRole())));
        rolePermissionRegistry.resolve(Set.of(user.getRole())).stream()
                .map(AuthorityNames::permission)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        return new PlatformUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.isActive(),
                !user.isLockedAt(now),
                Set.copyOf(authorities));
    }

    public UUID getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
