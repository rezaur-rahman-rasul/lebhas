package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.payment.cache.PaymentLockService;
import com.lebhas.creativesaas.payment.cache.PaymentWebhookIdempotencyService;
import com.lebhas.creativesaas.payment.application.dto.PaymentWebhookProcessingResult;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookLog;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentWebhookProcessor {

    private final PaymentTransactionService paymentTransactionService;
    private final PaymentStatusSynchronizer paymentStatusSynchronizer;
    private final PaymentWebhookLogService paymentWebhookLogService;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentWebhookIdempotencyService webhookIdempotencyService;
    private final PaymentLockService paymentLockService;

    public PaymentWebhookProcessor(
            PaymentTransactionService paymentTransactionService,
            PaymentStatusSynchronizer paymentStatusSynchronizer,
            PaymentWebhookLogService paymentWebhookLogService,
            PaymentEventProducer paymentEventProducer,
            PaymentWebhookIdempotencyService webhookIdempotencyService,
            PaymentLockService paymentLockService
    ) {
        this.paymentTransactionService = paymentTransactionService;
        this.paymentStatusSynchronizer = paymentStatusSynchronizer;
        this.paymentWebhookLogService = paymentWebhookLogService;
        this.paymentEventProducer = paymentEventProducer;
        this.webhookIdempotencyService = webhookIdempotencyService;
        this.paymentLockService = paymentLockService;
    }

    public PaymentWebhookProcessingResult process(
            PaymentProvider provider,
            PaymentWebhookLog log,
            PaymentWebhookVerificationResult verificationResult,
            boolean duplicate
    ) {
        if (duplicate && log.isProcessed()) {
            return result(provider, log, null, verificationResult, true, "Duplicate webhook already processed");
        }
        if (!verificationResult.verified()) {
            log = paymentWebhookLogService.markProcessedFailure(log, verificationResult.failureReason());
            paymentEventProducer.paymentWebhookProcessed(provider, log, null, verificationResult, duplicate);
            return result(provider, log, null, verificationResult, duplicate, "Webhook verification failed");
        }

        if (webhookIdempotencyService.alreadyProcessed(verificationResult.providerTransactionId())) {
            log = paymentWebhookLogService.markProcessed(log);
            paymentEventProducer.paymentWebhookProcessed(provider, log, null, verificationResult, true);
            return result(provider, log, null, verificationResult, true, "Duplicate webhook already processed");
        }
        RedisLockService.RedisLockToken webhookLock = paymentLockService
                .acquireWebhookLock(verificationResult.providerTransactionId())
                .orElse(null);
        if (verificationResult.providerTransactionId() != null && webhookLock == null) {
            return result(provider, log, null, verificationResult, true, "Webhook processing is already in progress");
        }
        try {
            PaymentTransaction transaction = findTransaction(provider, verificationResult)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Payment transaction not found for webhook"));
            PaymentTransaction synchronizedTransaction = paymentStatusSynchronizer.synchronize(transaction, verificationResult);
            paymentWebhookLogService.markProcessed(log);
            webhookIdempotencyService.rememberProcessed(
                    verificationResult.providerTransactionId(),
                    synchronizedTransaction.getId(),
                    log.getId());
            paymentEventProducer.paymentWebhookProcessed(provider, log, synchronizedTransaction, verificationResult, duplicate);
            return result(provider, log, synchronizedTransaction, verificationResult, duplicate, "Webhook processed");
        } finally {
            paymentLockService.release(webhookLock);
        }
    }

    private Optional<PaymentTransaction> findTransaction(
            PaymentProvider provider,
            PaymentWebhookVerificationResult verificationResult
    ) {
        Optional<PaymentTransaction> byProviderTransaction = paymentTransactionService.findByProviderTransactionId(
                provider.getId(),
                verificationResult.providerTransactionId());
        if (byProviderTransaction.isPresent()) {
            return byProviderTransaction;
        }
        Map<String, String> payload = verificationResult.providerPayload() == null ? Map.of() : verificationResult.providerPayload();
        Optional<PaymentTransaction> bySession = paymentTransactionService.findByProviderSessionId(
                provider.getId(),
                firstPresent(payload, "providerSessionId", "sessionId", "paymentSessionId"));
        if (bySession.isPresent()) {
            return bySession;
        }
        return parseUuid(firstPresent(payload, "paymentTransactionId", "idempotencyKey", "transactionId"))
                .map(paymentTransactionService::requireTransaction);
    }

    private String firstPresent(Map<String, String> payload, String... keys) {
        for (String key : keys) {
            String value = payload.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private PaymentWebhookProcessingResult result(
            PaymentProvider provider,
            PaymentWebhookLog log,
            PaymentTransaction transaction,
            PaymentWebhookVerificationResult verificationResult,
            boolean duplicate,
            String message
    ) {
        return new PaymentWebhookProcessingResult(
                log.getId(),
                transaction == null ? null : transaction.getId(),
                provider.getCode(),
                verificationResult.eventType(),
                verificationResult.verificationStatus(),
                transaction == null ? verificationResult.paymentStatus() : transaction.getStatus(),
                log.isProcessed(),
                duplicate,
                message
        );
    }
}
