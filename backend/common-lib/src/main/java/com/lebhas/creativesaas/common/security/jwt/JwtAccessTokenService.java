package com.lebhas.creativesaas.common.security.jwt;

import com.lebhas.creativesaas.common.security.AuthenticatedPrincipal;
import com.lebhas.creativesaas.common.security.JwtTokenParser;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.authorization.RolePermissionRegistry;
import com.lebhas.creativesaas.common.security.session.AccessTokenRevocationStore;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtAccessTokenService implements JwtTokenParser {

    private static final Logger log = LoggerFactory.getLogger(JwtAccessTokenService.class);
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_WORKSPACE_ID = "workspaceId";
    private static final String CLAIM_DEVICE_ID = "deviceId";
    private static final String CLAIM_TYPE = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final JwtProperties properties;
    private final RolePermissionRegistry rolePermissionRegistry;
    private final AccessTokenRevocationStore accessTokenRevocationStore;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtAccessTokenService(
            JwtProperties properties,
            RolePermissionRegistry rolePermissionRegistry,
            AccessTokenRevocationStore accessTokenRevocationStore,
            Clock clock
    ) {
        this.properties = properties;
        this.rolePermissionRegistry = rolePermissionRegistry;
        this.accessTokenRevocationStore = accessTokenRevocationStore;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecretBase64()));
    }

    public IssuedAccessToken generate(UserEntity user, UUID workspaceId, Role effectiveRole) {
        return generate(user, workspaceId, effectiveRole, null);
    }

    public IssuedAccessToken generate(UserEntity user, UUID workspaceId, Role effectiveRole, String deviceId) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getAccessTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        Set<Role> roles = Set.of(effectiveRole);
        Set<Permission> permissions = rolePermissionRegistry.resolve(roles);
        String token = Jwts.builder()
                .header().type("JWT").and()
                .id(tokenId)
                .issuer(properties.getIssuer())
                .subject(user.getId().toString())
                .issuedAt(java.util.Date.from(issuedAt))
                .expiration(java.util.Date.from(expiresAt))
                .claim(CLAIM_TYPE, ACCESS_TOKEN_TYPE)
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLES, roles.stream().map(Role::name).toList())
                .claim(CLAIM_WORKSPACE_ID, workspaceId == null ? null : workspaceId.toString())
                .claim(CLAIM_DEVICE_ID, deviceId)
                .signWith(signingKey)
                .compact();
        return new IssuedAccessToken(token, tokenId, expiresAt, roles, permissions);
    }

    @Override
    public Optional<AuthenticatedPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clockSkewSeconds(properties.getClockSkew().toSeconds())
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!ACCESS_TOKEN_TYPE.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            String tokenId = claims.getId();
            String subject = claims.getSubject();
            String email = claims.get(CLAIM_EMAIL, String.class);
            if (tokenId == null || tokenId.isBlank() || subject == null || subject.isBlank() || email == null || email.isBlank()) {
                return Optional.empty();
            }
            if (accessTokenRevocationStore.isRevoked(tokenId)) {
                return Optional.empty();
            }
            List<?> rawRoles = claims.get(CLAIM_ROLES, List.class);
            Set<Role> roles = (rawRoles == null ? List.of() : rawRoles).stream()
                    .map(roleValue -> Role.valueOf(String.valueOf(roleValue)))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            Set<Permission> permissions = rolePermissionRegistry.resolve(roles);
            UUID workspaceId = Optional.ofNullable(claims.get(CLAIM_WORKSPACE_ID, String.class))
                    .filter(value -> !value.isBlank())
                    .map(UUID::fromString)
                    .orElse(null);
            String deviceId = Optional.ofNullable(claims.get(CLAIM_DEVICE_ID, String.class))
                    .filter(value -> !value.isBlank())
                    .orElse(null);
            return Optional.of(new AuthenticatedPrincipal(
                    UUID.fromString(subject),
                    workspaceId,
                    deviceId,
                    email,
                    roles,
                    permissions,
                    tokenId,
                    claims.getExpiration().toInstant()));
        } catch (RuntimeException exception) {
            log.debug("JWT parsing failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }
}
