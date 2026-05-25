package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.payment.application.dto.PaymentWebhookCommand;
import com.lebhas.creativesaas.payment.application.dto.PaymentWebhookProcessingResult;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookLog;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookVerificationStatus;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class PaymentWebhookService {

    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentWebhookLogService paymentWebhookLogService;
    private final PaymentWebhookVerifier paymentWebhookVerifier;
    private final PaymentWebhookProcessor paymentWebhookProcessor;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentWebhookService(
            PaymentProviderRepository paymentProviderRepository,
            PaymentWebhookLogService paymentWebhookLogService,
            PaymentWebhookVerifier paymentWebhookVerifier,
            PaymentWebhookProcessor paymentWebhookProcessor,
            PaymentEventProducer paymentEventProducer
    ) {
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentWebhookLogService = paymentWebhookLogService;
        this.paymentWebhookVerifier = paymentWebhookVerifier;
        this.paymentWebhookProcessor = paymentWebhookProcessor;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Transactional
    public PaymentWebhookProcessingResult receiveWebhook(PaymentWebhookCommand command) {
        PaymentProvider provider = identifyProvider(command.providerCode());
        PaymentWebhookLogService.WebhookLogReceipt receipt = paymentWebhookLogService.storeReceived(
                provider,
                requirePayload(command.payload()),
                command.signature());
        PaymentWebhookLog log = receipt.log();
        paymentEventProducer.paymentWebhookReceived(provider, log, receipt.duplicate());
        if (receipt.duplicate() && log.isProcessed()) {
            return new PaymentWebhookProcessingResult(
                    log.getId(),
                    null,
                    provider.getCode(),
                    log.getWebhookEventType(),
                    log.getVerificationStatus(),
                    null,
                    true,
                    true,
                    "Duplicate webhook already processed");
        }

        try {
            PaymentWebhookVerificationResult verificationResult = paymentWebhookVerifier.verify(
                    provider,
                    command.payload(),
                    command.signature());
            log = paymentWebhookLogService.markVerification(log, verificationResult);
            return paymentWebhookProcessor.process(provider, log, verificationResult, receipt.duplicate());
        } catch (RuntimeException ex) {
            log = paymentWebhookLogService.markProcessedFailure(log, ex.getMessage());
            paymentEventProducer.paymentWebhookProcessed(provider, log, null, new PaymentWebhookVerificationResult(
                    false,
                    log.getVerificationStatus() == null ? PaymentWebhookVerificationStatus.FAILED : log.getVerificationStatus(),
                    log.getProviderTransactionId(),
                    log.getWebhookEventType(),
                    null,
                    ex.getMessage(),
                    java.util.Map.of()
            ), receipt.duplicate());
            return new PaymentWebhookProcessingResult(
                    log.getId(),
                    null,
                    provider.getCode(),
                    log.getWebhookEventType(),
                    log.getVerificationStatus(),
                    null,
                    log.isProcessed(),
                    receipt.duplicate(),
                    ex.getMessage());
        }
    }

    private PaymentProvider identifyProvider(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Payment provider code is required");
        }
        return paymentProviderRepository.findByCode(providerCode.trim().toUpperCase(Locale.ROOT))
                .filter(PaymentProvider::isEnabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Payment provider not found"));
    }

    private String requirePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Webhook payload is required");
        }
        return payload;
    }
}
