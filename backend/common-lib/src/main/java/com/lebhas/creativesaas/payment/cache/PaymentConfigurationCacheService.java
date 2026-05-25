package com.lebhas.creativesaas.payment.cache;

import com.lebhas.creativesaas.payment.application.dto.PaymentProviderConfigurationView;
import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import com.lebhas.creativesaas.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentConfigurationCacheService {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfigurationCacheService.class);

    private final RedisCacheService redisCacheService;
    private final PaymentRedisTtlStrategy ttlStrategy;

    public PaymentConfigurationCacheService(RedisCacheService redisCacheService, PaymentRedisTtlStrategy ttlStrategy) {
        this.redisCacheService = redisCacheService;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<PaymentProviderConfigurationView> getConfiguration(
            UUID providerId,
            PaymentEnvironmentType environmentType
    ) {
        return read(key(providerId, environmentType), PaymentProviderConfigurationView.class, "get_configuration");
    }

    public void cacheConfiguration(PaymentProviderConfigurationView configuration) {
        if (configuration == null || configuration.providerId() == null || configuration.environmentType() == null) {
            return;
        }
        String key = key(configuration.providerId(), configuration.environmentType());
        try {
            redisCacheService.set(key, configuration, ttlStrategy.configurationTtl());
        } catch (RuntimeException exception) {
            logFailure("cache_configuration", key, exception);
        }
    }

    public void invalidateConfiguration(UUID providerId, PaymentEnvironmentType environmentType) {
        if (providerId == null || environmentType == null) {
            return;
        }
        delete(key(providerId, environmentType), "invalidate_configuration");
    }

    private String key(UUID providerId, PaymentEnvironmentType environmentType) {
        return PaymentRedisKeys.paymentConfiguration(providerId, environmentType);
    }

    private <T> Optional<T> read(String key, Class<T> type, String operation) {
        try {
            return redisCacheService.get(key, type);
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
            return Optional.empty();
        }
    }

    private void delete(String key, String operation) {
        try {
            redisCacheService.delete(key);
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
        }
    }

    private void logFailure(String operation, String key, RuntimeException exception) {
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
        log.warn("payment_redis_failure operation={} key={} reason={}", operation, key, reason);
    }
}
