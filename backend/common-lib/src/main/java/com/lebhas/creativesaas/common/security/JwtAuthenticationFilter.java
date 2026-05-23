package com.lebhas.creativesaas.common.security;

import com.lebhas.creativesaas.common.constants.CommonHeaders;
import com.lebhas.creativesaas.common.security.authorization.WorkspaceGrantedAuthorityResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenParser tokenParser;
    private final WorkspaceGrantedAuthorityResolver workspaceGrantedAuthorityResolver;

    public JwtAuthenticationFilter(
            JwtTokenParser tokenParser,
            WorkspaceGrantedAuthorityResolver workspaceGrantedAuthorityResolver
    ) {
        this.tokenParser = tokenParser;
        this.workspaceGrantedAuthorityResolver = workspaceGrantedAuthorityResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveBearerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenParser.parse(token).ifPresent(this::authenticate);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(AuthenticatedPrincipal principal) {
        Set<Role> roles = principal.roles() == null ? Set.of() : principal.roles();
        Set<Permission> permissions = workspaceGrantedAuthorityResolver.resolve(principal);
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        roles.stream()
                .map(AuthorityNames::role)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        permissions.stream()
                .map(AuthorityNames::permission)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        AuthenticatedPrincipal authenticatedPrincipal = new AuthenticatedPrincipal(
                principal.userId(),
                principal.workspaceId(),
                principal.deviceId(),
                principal.email(),
                roles,
                permissions,
                principal.tokenId(),
                principal.expiresAt());
        var authentication = new UsernamePasswordAuthenticationToken(authenticatedPrincipal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(CommonHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
