package com.lebhas.creativesaas.payment.cache;

import com.lebhas.creativesaas.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentSessionCacheService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSessionCacheService.class);

    private final RedisCacheService redisCacheService;
    private final PaymentRedisTtlStrategy ttlStrategy;

    public PaymentSessionCacheService(RedisCacheService redisCacheService, PaymentRedisTtlStrategy ttlStrategy) {
        this.redisCacheService = redisCacheService;
        this.ttlStrategy = ttlStrategy;
    }

    public void cacheSession(PaymentSessionCacheEntry entry) {
        if (entry == null || entry.paymentTransactionId() == null) {
            return;
        }
        String key = PaymentRedisKeys.paymentSession(entry.paymentTransactionId());
        try {
            redisCacheService.set(key, entry, ttlStrategy.sessionTtl());
        } catch (RuntimeException exception) {
            logFailure("cache_session", key, exception);
        }
    }

    public Optional<PaymentSessionCacheEntry> get(UUID paymentTransactionId) {
        String key = PaymentRedisKeys.paymentSession(paymentTransactionId);
        try {
            return redisCacheService.get(key, PaymentSessionCacheEntry.class);
        } catch (RuntimeException exception) {
            logFailure("get_session", key, exception);
            return Optional.empty();
        }
    }

    public void invalidate(UUID paymentTransactionId) {
        if (paymentTransactionId == null) {
            return;
        }
        String key = PaymentRedisKeys.paymentSession(paymentTransactionId);
        try {
            redisCacheService.delete(key);
        } catch (RuntimeException exception) {
            logFailure("invalidate_session", key, exception);
        }
    }

    private void logFailure(String operation, String key, RuntimeException exception) {
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
        log.warn("payment_redis_failure operation={} key={} reason={}", operation, key, reason);
    }
}
