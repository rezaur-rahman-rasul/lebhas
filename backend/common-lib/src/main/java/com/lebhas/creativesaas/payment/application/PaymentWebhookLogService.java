package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookLog;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookVerificationStatus;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentWebhookLogRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class PaymentWebhookLogService {

    private static final String UNKNOWN_EVENT = "UNKNOWN";

    private final PaymentWebhookLogRepository paymentWebhookLogRepository;

    public PaymentWebhookLogService(PaymentWebhookLogRepository paymentWebhookLogRepository) {
        this.paymentWebhookLogRepository = paymentWebhookLogRepository;
    }

    public WebhookLogReceipt storeReceived(PaymentProvider provider, String payload, String signature) {
        String signatureHash = signatureHash(provider.getCode(), payload, signature);
        return paymentWebhookLogRepository.findByProviderIdAndSignatureHash(provider.getId(), signatureHash)
                .map(log -> new WebhookLogReceipt(log, true))
                .orElseGet(() -> new WebhookLogReceipt(paymentWebhookLogRepository.save(PaymentWebhookLog.create(
                        provider.getId(),
                        null,
                        UNKNOWN_EVENT,
                        payload,
                        signatureHash,
                        PaymentWebhookVerificationStatus.PENDING,
                        false,
                        null
                )), false));
    }

    public PaymentWebhookLog markVerification(PaymentWebhookLog log, PaymentWebhookVerificationResult result) {
        log.markVerification(
                result.verificationStatus(),
                result.providerTransactionId(),
                result.eventType(),
                result.failureReason());
        return paymentWebhookLogRepository.save(log);
    }

    public PaymentWebhookLog markProcessed(PaymentWebhookLog log) {
        log.markProcessed();
        return paymentWebhookLogRepository.save(log);
    }

    public PaymentWebhookLog markProcessedFailure(PaymentWebhookLog log, String failureReason) {
        log.markProcessedFailure(failureReason);
        return paymentWebhookLogRepository.save(log);
    }

    private String signatureHash(String providerCode, String payload, String signature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((providerCode + "\n" + nullToBlank(signature) + "\n" + nullToBlank(payload))
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte current : hash) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    public record WebhookLogReceipt(
            PaymentWebhookLog log,
            boolean duplicate
    ) {
    }
}
