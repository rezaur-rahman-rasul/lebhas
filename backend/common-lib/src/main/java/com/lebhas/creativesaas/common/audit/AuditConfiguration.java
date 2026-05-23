package com.lebhas.creativesaas.common.audit;

import com.lebhas.creativesaas.common.security.AuthenticatedPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@ConditionalOnProperty(prefix = "platform.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditConfiguration {

    private static final String SYSTEM_AUDITOR = "system";
    private static final String ANONYMOUS = "anonymousUser";

    @Bean
    AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .map(this::resolveAuditor)
                .filter(auditor -> !auditor.isBlank())
                .or(() -> Optional.of(SYSTEM_AUDITOR));
    }

    private String resolveAuditor(Object principal) {
        if (principal instanceof AuthenticatedPrincipal authenticatedPrincipal) {
            return authenticatedPrincipal.userId().toString();
        }
        if (principal instanceof String name && !ANONYMOUS.equals(name)) {
            return name;
        }
        return SYSTEM_AUDITOR;
    }
}
