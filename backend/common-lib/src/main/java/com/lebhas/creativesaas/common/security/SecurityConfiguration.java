package com.lebhas.creativesaas.common.security;

import com.lebhas.creativesaas.common.constants.CommonHeaders;
import com.lebhas.creativesaas.common.security.rate.AuthenticationRateLimitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.lebhas.creativesaas.common.security.session.AccessTokenRevocationStore;
import com.lebhas.creativesaas.common.security.session.AccessTokenRevocationRepository;
import com.lebhas.creativesaas.common.security.session.CompositeAccessTokenRevocationStore;
import com.lebhas.creativesaas.common.security.session.PersistentAccessTokenRevocationStore;
import com.lebhas.creativesaas.common.security.session.RedisAccessTokenRevocationStore;
import com.lebhas.creativesaas.redis.RedisSessionService;

import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({
        SecurityProperties.class,
        com.lebhas.creativesaas.common.security.jwt.JwtProperties.class,
        AuthenticationRateLimitProperties.class
})
public class SecurityConfiguration {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/health",
            "/health/live",
            "/health/ready",
            "/liveness",
            "/readiness",
            "/actuator/**",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/pricing-plans/public",
            "/api/v1/credit-packages/public",
            "/api/v1/public/share/**",
            "/api/v1/payments/webhooks/**",
            "/internal/storage/local/assets/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfiguration(
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource,
            DaoAuthenticationProvider daoAuthenticationProvider
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(daoAuthenticationProvider)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(properties.getAllowedOrigins());
        configuration.setAllowedMethods(properties.getAllowedMethods());
        configuration.setAllowedHeaders(properties.getAllowedHeaders());
        configuration.setExposedHeaders(List.of(CommonHeaders.CORRELATION_ID));
        configuration.setAllowCredentials(properties.isAllowCredentials());
        configuration.setMaxAge(properties.getMaxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider(
            PlatformUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    java.time.Clock clock() {
        return java.time.Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(AccessTokenRevocationStore.class)
    AccessTokenRevocationStore accessTokenRevocationStore(
            AccessTokenRevocationRepository accessTokenRevocationRepository,
            RedisSessionService redisSessionService,
            java.time.Clock clock
    ) {
        PersistentAccessTokenRevocationStore persistentStore =
                new PersistentAccessTokenRevocationStore(accessTokenRevocationRepository, clock);
        RedisAccessTokenRevocationStore redisStore =
                new RedisAccessTokenRevocationStore(redisSessionService, clock);
        return new CompositeAccessTokenRevocationStore(persistentStore, redisStore);
    }

    @Bean
    @ConditionalOnMissingBean(JwtTokenParser.class)
    JwtTokenParser jwtTokenParserFallback() {
        return new NoopJwtTokenParser();
    }
}
