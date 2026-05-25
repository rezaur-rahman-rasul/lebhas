package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.payment.cache.PaymentProviderCacheService;
import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderConfiguration;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentProviderConfigurationRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentProviderRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Locale;

@Service
public class PaymentProviderResolver {

    private static final PaymentEnvironmentType DEFAULT_ENVIRONMENT = PaymentEnvironmentType.SANDBOX;

    private final PaymentProviderRepository providerRepository;
    private final PaymentProviderConfigurationRepository configurationRepository;
    private final PaymentProviderCacheService providerCacheService;

    public PaymentProviderResolver(
            PaymentProviderRepository providerRepository,
            PaymentProviderConfigurationRepository configurationRepository,
            PaymentProviderCacheService providerCacheService
    ) {
        this.providerRepository = providerRepository;
        this.configurationRepository = configurationRepository;
        this.providerCacheService = providerCacheService;
    }

    public ResolvedPaymentProvider resolve(PaymentSessionRequest request) {
        PaymentEnvironmentType environmentType = environment(request.environmentType());
        String preferredProviderCode = normalizeNullable(request.preferredProviderCode());
        if (preferredProviderCode != null) {
            return resolveByCode(preferredProviderCode, environmentType);
        }
        java.util.Optional<ResolvedPaymentProvider> cachedProvider = resolveCachedActiveProvider(environmentType);
        if (cachedProvider.isPresent()) {
            return cachedProvider.get();
        }
        ResolvedPaymentProvider resolvedProvider = providerRepository.findAllByEnabledTrueOrderByPriorityAscNameAsc().stream()
                .filter(provider -> enabledForEnvironment(provider, environmentType))
                .map(provider -> resolveConfiguration(provider, environmentType))
                .flatMap(java.util.Optional::stream)
                .min(Comparator.comparingInt(resolved -> resolved.provider().getPriority()))
                .orElseThrow(() -> noProvider(environmentType));
        providerCacheService.cacheActiveProvider(resolvedProvider.provider().getId(), resolvedProvider.provider().getCode());
        return resolvedProvider;
    }

    public ResolvedPaymentProvider resolve(PaymentVerificationRequest request) {
        PaymentEnvironmentType environmentType = environment(request.environmentType());
        String providerCode = normalizeNullable(request.providerCode());
        if (providerCode == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Payment provider code is required for verification");
        }
        return resolveByCode(providerCode, environmentType);
    }

    public ResolvedPaymentProvider resolveForWebhook(String providerCode) {
        String normalizedCode = normalizeNullable(providerCode);
        if (normalizedCode == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Payment provider code is required for webhook verification");
        }
        PaymentProvider provider = providerRepository.findByCode(normalizedCode)
                .filter(PaymentProvider::isEnabled)
                .orElseThrow(() -> noProvider(normalizedCode));
        PaymentEnvironmentType environmentType = provider.isLiveEnabled()
                ? PaymentEnvironmentType.LIVE
                : PaymentEnvironmentType.SANDBOX;
        if (!enabledForEnvironment(provider, environmentType)) {
            throw noProvider(normalizedCode);
        }
        return resolveConfiguration(provider, environmentType).orElseThrow(() -> noProvider(environmentType));
    }

    private ResolvedPaymentProvider resolveByCode(String providerCode, PaymentEnvironmentType environmentType) {
        PaymentProvider provider = providerRepository.findByCode(providerCode)
                .filter(PaymentProvider::isEnabled)
                .filter(current -> enabledForEnvironment(current, environmentType))
                .orElseThrow(() -> noProvider(providerCode));
        return resolveConfiguration(provider, environmentType).orElseThrow(() -> noProvider(environmentType));
    }

    private java.util.Optional<ResolvedPaymentProvider> resolveConfiguration(
            PaymentProvider provider,
            PaymentEnvironmentType environmentType
    ) {
        return configurationRepository.findByProviderIdAndEnvironmentTypeAndActiveTrue(provider.getId(), environmentType)
                .map(configuration -> new ResolvedPaymentProvider(provider, configuration));
    }

    private java.util.Optional<ResolvedPaymentProvider> resolveCachedActiveProvider(PaymentEnvironmentType environmentType) {
        return providerCacheService.getActiveProvider()
                .flatMap(cached -> providerRepository.findById(cached.providerId()))
                .filter(PaymentProvider::isEnabled)
                .filter(provider -> enabledForEnvironment(provider, environmentType))
                .flatMap(provider -> resolveConfiguration(provider, environmentType));
    }

    private boolean enabledForEnvironment(PaymentProvider provider, PaymentEnvironmentType environmentType) {
        return switch (environmentType) {
            case SANDBOX -> provider.isSandboxEnabled();
            case LIVE -> provider.isLiveEnabled();
        };
    }

    private PaymentEnvironmentType environment(PaymentEnvironmentType environmentType) {
        return environmentType == null ? DEFAULT_ENVIRONMENT : environmentType;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private BusinessException noProvider(PaymentEnvironmentType environmentType) {
        return new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "No active payment provider is configured for " + environmentType.name().toLowerCase(Locale.ROOT)
        );
    }

    private BusinessException noProvider(String providerCode) {
        return new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "Payment provider is not enabled or configured: " + providerCode
        );
    }

    public record ResolvedPaymentProvider(
            PaymentProvider provider,
            PaymentProviderConfiguration configuration
    ) {
    }
}
