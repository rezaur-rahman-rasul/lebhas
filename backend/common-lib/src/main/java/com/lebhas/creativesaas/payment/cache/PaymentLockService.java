package com.lebhas.creativesaas.payment.cache;

import com.lebhas.creativesaas.redis.RedisLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentLockService {

    private static final Logger log = LoggerFactory.getLogger(PaymentLockService.class);

    private final RedisLockService redisLockService;
    private final PaymentRedisTtlStrategy ttlStrategy;

    public PaymentLockService(RedisLockService redisLockService, PaymentRedisTtlStrategy ttlStrategy) {
        this.redisLockService = redisLockService;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<RedisLockService.RedisLockToken> acquirePaymentLock(UUID paymentTransactionId) {
        if (paymentTransactionId == null) {
            return Optional.empty();
        }
        String key = PaymentRedisKeys.paymentLock(paymentTransactionId);
        try {
            return redisLockService.acquire(key, ttlStrategy.paymentLockTtl());
        } catch (RuntimeException exception) {
            logFailure("acquire_payment_lock", key, exception);
            return Optional.empty();
        }
    }

    public Optional<RedisLockService.RedisLockToken> acquireWebhookLock(String providerTransactionId) {
        if (providerTransactionId == null || providerTransactionId.isBlank()) {
            return Optional.empty();
        }
        String key = PaymentRedisKeys.paymentWebhookLock(providerTransactionId);
        try {
            return redisLockService.acquire(key, ttlStrategy.webhookLockTtl());
        } catch (RuntimeException exception) {
            logFailure("acquire_webhook_lock", key, exception);
            return Optional.empty();
        }
    }

    public void release(RedisLockService.RedisLockToken token) {
        if (token == null) {
            return;
        }
        redisLockService.releaseQuietly(token);
    }

    private void logFailure(String operation, String key, RuntimeException exception) {
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
        log.warn("payment_redis_failure operation={} key={} reason={}", operation, key, reason);
    }
}
