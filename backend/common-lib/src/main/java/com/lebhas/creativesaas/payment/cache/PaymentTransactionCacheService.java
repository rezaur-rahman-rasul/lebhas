package com.lebhas.creativesaas.payment.cache;

import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentTransactionCacheService {

    private static final Logger log = LoggerFactory.getLogger(PaymentTransactionCacheService.class);

    private final RedisCacheService redisCacheService;
    private final PaymentRedisTtlStrategy ttlStrategy;

    public PaymentTransactionCacheService(RedisCacheService redisCacheService, PaymentRedisTtlStrategy ttlStrategy) {
        this.redisCacheService = redisCacheService;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<PaymentTransactionCacheEntry> getTransaction(UUID paymentTransactionId) {
        String key = PaymentRedisKeys.paymentTransaction(paymentTransactionId);
        try {
            return redisCacheService.get(key, PaymentTransactionCacheEntry.class);
        } catch (RuntimeException exception) {
            logFailure("get_transaction", key, exception);
            return Optional.empty();
        }
    }

    public void cacheTransaction(PaymentTransaction transaction) {
        if (transaction == null || transaction.getId() == null) {
            return;
        }
        PaymentTransactionCacheEntry entry = new PaymentTransactionCacheEntry(
                transaction.getId(),
                transaction.getWorkspaceId(),
                transaction.getUserId(),
                transaction.getProviderId(),
                transaction.getPaymentPurpose(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getProviderTransactionId(),
                transaction.getProviderSessionId(),
                transaction.getStatus(),
                Instant.now()
        );
        write(PaymentRedisKeys.paymentTransaction(transaction.getId()), entry, "cache_transaction");
    }

    public void invalidateTransaction(UUID paymentTransactionId) {
        if (paymentTransactionId == null) {
            return;
        }
        delete(PaymentRedisKeys.paymentTransaction(paymentTransactionId), "invalidate_transaction");
    }

    private void write(String key, Object value, String operation) {
        try {
            redisCacheService.set(key, value, ttlStrategy.transactionTtl());
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
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
        log.warn("payment_redis_failure operation={} key={} reason={}", operation, key, reason);
    }
}
