package com.lebhas.creativesaas.common.config;

import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.asset.storage.StorageProperties;
import com.lebhas.creativesaas.common.security.jwt.JwtProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

@Component
public class ProductionReadinessGuard implements SmartInitializingSingleton {

    private static final String DEV_JWT_SECRET =
            "Q3JlYXRpdmVTYWFTUGxhdGZvcm1EZXYyMDI2U2lnbmluZ1NlY3JldEJhc2U2NEtleUZvckxvY2FsVXNlT25seQ==";

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final StorageProperties storageProperties;

    public ProductionReadinessGuard(
            Environment environment,
            JwtProperties jwtProperties,
            StorageProperties storageProperties
    ) {
        this.environment = environment;
        this.jwtProperties = jwtProperties;
        this.storageProperties = storageProperties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!isProductionProfileActive()) {
            return;
        }
        requireProductionJwtSecret();
        requireRemoteObjectStorage();
        requireRealAiProvider();
    }

    private boolean isProductionProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> Objects.equals("production", profile));
    }

    private void requireProductionJwtSecret() {
        String secret = jwtProperties.getSecretBase64();
        if (secret == null || secret.isBlank() || DEV_JWT_SECRET.equals(secret)) {
            throw new IllegalStateException("Production profile requires JWT_SECRET_BASE64 with a non-development signing key");
        }
    }

    private void requireRemoteObjectStorage() {
        if (storageProperties.getProvider() == StorageProvider.LOCAL) {
            throw new IllegalStateException("Production profile must not use LOCAL storage; configure Cloudflare R2 or S3");
        }
    }

    private void requireRealAiProvider() {
        boolean mockEnabled = environment.getProperty("platform.ai.generation.mock.enabled", Boolean.class, true);
        if (mockEnabled) {
            throw new IllegalStateException("Production profile must disable AI_MOCK_ENABLED and use a configured provider route");
        }
    }
}
