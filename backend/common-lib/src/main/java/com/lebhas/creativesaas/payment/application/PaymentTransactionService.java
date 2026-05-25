package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.cache.PaymentTransactionCacheService;
import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentTransactionCacheService paymentTransactionCacheService;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentTransactionService(
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentTransactionCacheService paymentTransactionCacheService,
            PaymentEventProducer paymentEventProducer
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentTransactionCacheService = paymentTransactionCacheService;
        this.paymentEventProducer = paymentEventProducer;
    }

    public PaymentTransaction createTransaction(
            UUID workspaceId,
            UUID userId,
            UUID providerId,
            PaymentPurpose paymentPurpose,
            String referenceType,
            UUID referenceId,
            BigDecimal amount,
            String currency
    ) {
        PaymentTransaction transaction = paymentTransactionRepository.save(PaymentTransaction.create(
                workspaceId,
                userId,
                providerId,
                paymentPurpose,
                referenceType,
                referenceId,
                amount,
                currency
        ));
        paymentTransactionCacheService.cacheTransaction(transaction);
        paymentEventProducer.paymentTransactionInitiated(transaction);
        return transaction;
    }

    public PaymentTransaction markPending(PaymentTransaction transaction, PaymentSessionResponse sessionResponse) {
        transaction.markPending(sessionResponse.providerSessionId(), sessionResponse.providerTransactionId());
        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        paymentTransactionCacheService.cacheTransaction(saved);
        return saved;
    }

    public PaymentTransaction markSuccess(PaymentTransaction transaction, String providerTransactionId) {
        transaction.markSuccess(providerTransactionId);
        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        paymentTransactionCacheService.cacheTransaction(saved);
        paymentEventProducer.paymentTransactionSucceeded(saved);
        return saved;
    }

    public PaymentTransaction markFailed(PaymentTransaction transaction, String failureReason) {
        transaction.markFailed(failureReason);
        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        paymentTransactionCacheService.cacheTransaction(saved);
        paymentEventProducer.paymentTransactionFailed(saved);
        return saved;
    }

    public PaymentTransaction markCancelled(PaymentTransaction transaction) {
        transaction.markCancelled();
        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        paymentTransactionCacheService.cacheTransaction(saved);
        paymentEventProducer.paymentTransactionCancelled(saved);
        return saved;
    }

    public PaymentTransaction requireTransaction(UUID paymentTransactionId) {
        return paymentTransactionRepository.findById(paymentTransactionId)
                .orElseThrow(() -> new com.lebhas.creativesaas.common.exception.BusinessException(
                        com.lebhas.creativesaas.common.exception.ErrorCode.RESOURCE_NOT_FOUND,
                        "Payment transaction not found"));
    }

    public java.util.Optional<PaymentTransaction> findByProviderTransactionId(UUID providerId, String providerTransactionId) {
        if (providerId == null || providerTransactionId == null || providerTransactionId.isBlank()) {
            return java.util.Optional.empty();
        }
        return paymentTransactionRepository.findByProviderIdAndProviderTransactionId(providerId, providerTransactionId.trim());
    }

    public java.util.Optional<PaymentTransaction> findByProviderSessionId(UUID providerId, String providerSessionId) {
        if (providerId == null || providerSessionId == null || providerSessionId.isBlank()) {
            return java.util.Optional.empty();
        }
        return paymentTransactionRepository.findByProviderIdAndProviderSessionId(providerId, providerSessionId.trim());
    }
}
