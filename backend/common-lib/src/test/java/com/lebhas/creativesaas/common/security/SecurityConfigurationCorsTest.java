package com.lebhas.creativesaas.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationCorsTest {

    private final SecurityConfiguration securityConfiguration = new SecurityConfiguration(null, null);

    @Test
    void shouldAllowLoopbackAliasesForLocalFrontendOrigins() {
        SecurityProperties properties = new SecurityProperties();
        properties.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://127.0.0.1:4200",
                "http://[::1]:4200",
                "http://localhost:4300",
                "http://127.0.0.1:4300",
                "http://[::1]:4300"));

        CorsConfigurationSource configurationSource = securityConfiguration.corsConfigurationSource(properties);

        assertOriginAllowed(configurationSource, "http://localhost:4200");
        assertOriginAllowed(configurationSource, "http://127.0.0.1:4200");
        assertOriginAllowed(configurationSource, "http://[::1]:4200");
        assertOriginAllowed(configurationSource, "http://localhost:4300");
        assertOriginAllowed(configurationSource, "http://127.0.0.1:4300");
        assertOriginAllowed(configurationSource, "http://[::1]:4300");
        assertThat(resolveConfiguration(configurationSource).checkOrigin("http://evil.example")).isNull();
    }

    private void assertOriginAllowed(CorsConfigurationSource configurationSource, String origin) {
        assertThat(resolveConfiguration(configurationSource).checkOrigin(origin)).isEqualTo(origin);
    }

    private CorsConfiguration resolveConfiguration(CorsConfigurationSource configurationSource) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/register");
        request.addHeader("Access-Control-Request-Method", "POST");
        return configurationSource.getCorsConfiguration(request);
    }
}
