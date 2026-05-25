package com.lebhas.creativesaas.payment.cache;

import com.lebhas.creativesaas.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentWebhookIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookIdempotencyService.class);

    private final RedisCacheService redisCacheService;
    private final PaymentRedisTtlStrategy ttlStrategy;

    public PaymentWebhookIdempotencyService(RedisCacheService redisCacheService, PaymentRedisTtlStrategy ttlStrategy) {
        this.redisCacheService = redisCacheService;
        this.ttlStrategy = ttlStrategy;
    }

    public boolean alreadyProcessed(String providerTransactionId) {
        if (providerTransactionId == null || providerTransactionId.isBlank()) {
            return false;
        }
        String key = PaymentRedisKeys.paymentWebhook(providerTransactionId);
        try {
            return redisCacheService.get(key, PaymentWebhookMemory.class)
                    .map(PaymentWebhookMemory::processed)
                    .orElse(false);
        } catch (RuntimeException exception) {
            logFailure("webhook_already_processed", key, exception);
            return false;
        }
    }

    public void rememberProcessed(String providerTransactionId, UUID paymentTransactionId, UUID webhookLogId) {
        if (providerTransactionId == null || providerTransactionId.isBlank()) {
            return;
        }
        String key = PaymentRedisKeys.paymentWebhook(providerTransactionId);
        try {
            redisCacheService.set(
                    key,
                    new PaymentWebhookMemory(paymentTransactionId, webhookLogId, true, Instant.now()),
                    ttlStrategy.webhookIdempotencyTtl());
        } catch (RuntimeException exception) {
            logFailure("webhook_remember_processed", key, exception);
        }
    }

    public Optional<PaymentWebhookMemory> getMemory(String providerTransactionId) {
        if (providerTransactionId == null || providerTransactionId.isBlank()) {
            return Optional.empty();
        }
        String key = PaymentRedisKeys.paymentWebhook(providerTransactionId);
        try {
            return redisCacheService.get(key, PaymentWebhookMemory.class);
        } catch (RuntimeException exception) {
            logFailure("webhook_get_memory", key, exception);
            return Optional.empty();
        }
    }

    private void logFailure(String operation, String key, RuntimeException exception) {
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
        log.warn("payment_redis_failure operation={} key={} reason={}", operation, key, reason);
    }

    public record PaymentWebhookMemory(
            UUID paymentTransactionId,
            UUID webhookLogId,
            boolean processed,
            Instant processedAt
    ) {
    }
}
