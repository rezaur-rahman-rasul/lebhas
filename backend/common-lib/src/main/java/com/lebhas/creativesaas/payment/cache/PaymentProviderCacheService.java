package com.lebhas.creativesaas.payment.cache;

import com.lebhas.creativesaas.payment.application.dto.PaymentProviderView;
import com.lebhas.creativesaas.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentProviderCacheService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProviderCacheService.class);

    private final RedisCacheService redisCacheService;
    private final PaymentRedisTtlStrategy ttlStrategy;

    public PaymentProviderCacheService(RedisCacheService redisCacheService, PaymentRedisTtlStrategy ttlStrategy) {
        this.redisCacheService = redisCacheService;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<PaymentProviderView> getProvider(UUID providerId) {
        return read(PaymentRedisKeys.paymentProvider(providerId), PaymentProviderView.class, "get_provider");
    }

    public void cacheProvider(PaymentProviderView provider) {
        if (provider == null || provider.id() == null) {
            return;
        }
        write(PaymentRedisKeys.paymentProvider(provider.id()), provider, "cache_provider");
    }

    public Optional<ActivePaymentProviderCacheEntry> getActiveProvider() {
        return read(PaymentRedisKeys.activePaymentProvider(), ActivePaymentProviderCacheEntry.class, "get_active_provider");
    }

    public void cacheActiveProvider(UUID providerId, String providerCode) {
        if (providerId == null || providerCode == null || providerCode.isBlank()) {
            return;
        }
        try {
            redisCacheService.set(
                    PaymentRedisKeys.activePaymentProvider(),
                    new ActivePaymentProviderCacheEntry(providerId, providerCode.trim()),
                    ttlStrategy.activeProviderTtl());
        } catch (RuntimeException exception) {
            logFailure("cache_active_provider", PaymentRedisKeys.activePaymentProvider(), exception);
        }
    }

    public void invalidateProvider(UUID providerId) {
        if (providerId == null) {
            return;
        }
        delete(PaymentRedisKeys.paymentProvider(providerId), "invalidate_provider");
        delete(PaymentRedisKeys.activePaymentProvider(), "invalidate_active_provider");
    }

    public void invalidateActiveProvider() {
        delete(PaymentRedisKeys.activePaymentProvider(), "invalidate_active_provider");
    }

    private <T> Optional<T> read(String key, Class<T> type, String operation) {
        try {
            return redisCacheService.get(key, type);
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
            return Optional.empty();
        }
    }

    private void write(String key, Object value, String operation) {
        try {
            redisCacheService.set(key, value, ttlStrategy.providerTtl());
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
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
        log.warn("payment_redis_failure operation={} key={} reason={}", operation, key, reason(exception));
    }

    private String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }

    public record ActivePaymentProviderCacheEntry(UUID providerId, String providerCode) {
    }
}
